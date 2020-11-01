/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.code;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.util.Iterator;

/**
 * 作用域（Scope）是Java语言的一部分，大多数作用域都以花括号分隔，因此每个JCBlock对象都能形成具体的作用域。
 * 同一个符号在不同的作用域中可能指向不同的实体。符号的有效区域始于名称的定义语句，以定义语句所在的作用域末端为结束。
 * 在Javac中，作用域可通过Scope类来表示。<p>
 * A scope represents an area of visibility in a Java program. The
 *  Scope class is a container for symbols which provides
 *  efficient access to symbols given their names. Scopes are implemented
 *  as hash tables with "open addressing" and "double hashing".
 *  Scopes can be nested; the next field of a scope points
 *  to its next outer scope. Nested scopes can share their hash tables.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Scope {

    /**
     * shared值为1，则表示next所指向的Scope对象与当前Scope对象共享同一个table数组。<p>
     * The number of scopes that share this scope's hash table.
     */
    private int shared;

    /**
     * 由于作用域可以嵌套，因而可通过next来指向外层嵌套的作用域，并且每个作用域都有所属的符号owner。<p>
     * Next enclosing scope (with whom this scope may share a hashtable)
     */
    public Scope next;

    /** The scope's owner.
     */
    public Symbol owner;

    /**
     * table数组用来存储作用域内定义的符号。一个作用域内定义的多个符号用数组来存储，不过并不是直接存储Symbol对象，而是将Symbol对象进一步封装为Entry对象，
     * 然后存储到table数组中。由于table数组专门用来存储符号，因而也可以直接叫符号表。同一个作用域内定义的所有符号会形成单链表，
     * elems保存了这个单链表的首个Entry对象，nelems保存了单链表中Entry对象的总数.<p>
     * A hash table for the scope's entries.
     */
    Entry[] table;

    /** Mask for hash codes, always equal to (table.length - 1).
     */
    int hashMask;

    /**
     * 同一个作用域内定义的所有符号会形成单链表，elems保存了这个单链表的首个Entry对象.<p>
     * A linear list that also contains all entries in
     *  reverse order of appearance (i.e later entries are pushed on top).
     */
    public Entry elems;

    /**
     * 同一个作用域内定义的所有符号会形成单链表，nelems保存了单链表中Entry对象的总数.<p>
     * The number of elements in this scope.
     * This includes deleted elements, whose value is the sentinel.
     */
    int nelems = 0;

    /** A list of scopes to be notified if items are to be removed from this scope.
     */
    List<ScopeListener> listeners = List.nil();

    /** Use as a "not-found" result for lookup.
     * Also used to mark deleted entries in the table.
     */
    private static final Entry sentinel = new Entry(null, null, null, null);

    /** The hash table's initial size.
     */
    private static final int INITIAL_SIZE = 0x10;

    /** A value for the empty scope.
     */
    public static final Scope emptyScope = new Scope(null, null, new Entry[]{});

    /** Construct a new scope, within scope next, with given owner, using
     *  given table. The table's length must be an exponent of 2.
     */
    private Scope(Scope next, Symbol owner, Entry[] table) {
        this.next = next;
        Assert.check(emptyScope == null || owner != null);
        this.owner = owner;
        this.table = table;
        this.hashMask = table.length - 1;
    }

    /** Convenience constructor used for dup and dupUnshared. */
    private Scope(Scope next, Symbol owner, Entry[] table, int nelems) {
        this(next, owner, table);
        this.nelems = nelems;
    }

    /** Construct a new scope, within scope next, with given owner,
     *  using a fresh table of length INITIAL_SIZE.
     */
    public Scope(Symbol owner) {
        this(null, owner, new Entry[INITIAL_SIZE]);
    }

    /** Construct a fresh scope within this scope, with same owner,
     *  which shares its table with the outer scope. Used in connection with
     *  method leave if scope access is stack-like in order to avoid allocation
     *  of fresh tables.
     */
    public Scope dup() {
        return dup(this.owner);
    }

    /** Construct a fresh scope within this scope, with new owner,
     *  which shares its table with the outer scope. Used in connection with
     *  method leave if scope access is stack-like in order to avoid allocation
     *  of fresh tables.
     */
    public Scope dup(Symbol newOwner) {
        Scope result = new Scope(this, newOwner, this.table, this.nelems);
        shared++;
        // System.out.println("====> duping scope " + this.hashCode() + " owned by " + newOwner + " to " + result.hashCode());
        // new Error().printStackTrace(System.out);
        return result;
    }

    /** Construct a fresh scope within this scope, with same owner,
     *  with a new hash table, whose contents initially are those of
     *  the table of its outer scope.
     */
    public Scope dupUnshared() {
        return new Scope(this, this.owner, this.table.clone(), this.nelems);
    }

    /**
     * 如果离开相关的作用域，则应该调用leave()方法删除对应作用域内定义的符号<p>
     * Remove all entries of this scope from its table, if shared with next.
     */
    public Scope leave() {
        /*当前的符号表与外层作用域使用的不是同一个符号表时，不需要操作当前的符号表，直接丢弃即可，否则就需要从当前的符号表中删除相关的符号。
        当前作用域中定义的所有符号都通过sibling连接为单链表，因此只需要从单链表的头部elems开始删除即可，同时也要更新外层作用域的nelems值。*/
        Assert.check(shared == 0);
        if (table != next.table) {
            return next;
        }
        while (elems != null) {
            int hash = getIndex(elems.sym.name);
            Entry e = table[hash];
            Assert.check(e == elems, elems.sym);
            table[hash] = elems.shadowed;
            elems = elems.sibling;
        }
        Assert.check(next.shared > 0);
        next.shared--;
        next.nelems = nelems;
        // System.out.println("====> leaving scope " + this.hashCode() + " owned by " + this.owner + " to " + next.hashCode());
        // new Error().printStackTrace(System.out);
        return next;
    }

    /** Double size of hash table.
     */
    private void dble() {
        Assert.check(shared == 0);
        Entry[] oldtable = table;
        Entry[] newtable = new Entry[oldtable.length * 2];
        for (Scope s = this; s != null; s = s.next) {
            if (s.table == oldtable) {
                Assert.check(s == this || s.shared != 0);
                s.table = newtable;
                s.hashMask = newtable.length - 1;
            }
        }
        int n = 0;
        for (int i = oldtable.length; --i >= 0; ) {
            Entry e = oldtable[i];
            if (e != null && e != sentinel) {
                table[getIndex(e.sym.name)] = e;
                n++;
            }
        }
        // We don't need to update nelems for shared inherited scopes,
        // since that gets handled by leave().
        nelems = n;
    }

    /**
     * 向符号表里添加一个符号<p>
     * Enter symbol sym in this scope.
     */
    public void enter(Symbol sym) {
        Assert.check(shared == 0);
        enter(sym, this);
    }

    public void enter(Symbol sym, Scope s) {
        enter(sym, s, s);
    }

    /**
     * Enter symbol sym in this scope, but mark that it comes from
     * given scope `s' accessed through `origin'.  The last two
     * arguments are only used in import scopes.
     */
    public void enter(Symbol sym, Scope s, Scope origin) {
        Assert.check(shared == 0);
        if (nelems * 3 >= hashMask * 2)
            dble();
        int hash = getIndex(sym.name);
        Entry old = table[hash];
        if (old == null) {
            old = sentinel;
            nelems++;
        }
        Entry e = makeEntry(sym, old, elems, s, origin);
        table[hash] = e;
        elems = e;

        //notify listeners
        for (List<ScopeListener> l = listeners; l.nonEmpty(); l = l.tail) {
            l.head.symbolAdded(sym, this);
        }
    }

    Entry makeEntry(Symbol sym, Entry shadowed, Entry sibling, Scope scope, Scope origin) {
        return new Entry(sym, shadowed, sibling, scope);
    }


    public interface ScopeListener {
        public void symbolAdded(Symbol sym, Scope s);
        public void symbolRemoved(Symbol sym, Scope s);
    }

    public void addScopeListener(ScopeListener sl) {
        listeners = listeners.prepend(sl);
    }

    /** Remove symbol from this scope.  Used when an inner class
     *  attribute tells us that the class isn't a package member.
     */
    public void remove(Symbol sym) {
        Assert.check(shared == 0);
        Entry e = lookup(sym.name);
        if (e.scope == null) return;

        // remove e from table and shadowed list;
        int i = getIndex(sym.name);
        Entry te = table[i];
        if (te == e)
            table[i] = e.shadowed;
        else while (true) {
            if (te.shadowed == e) {
                te.shadowed = e.shadowed;
                break;
            }
            te = te.shadowed;
        }

        // remove e from elems and sibling list
        te = elems;
        if (te == e)
            elems = e.sibling;
        else while (true) {
            if (te.sibling == e) {
                te.sibling = e.sibling;
                break;
            }
            te = te.sibling;
        }

        //notify listeners
        for (List<ScopeListener> l = listeners; l.nonEmpty(); l = l.tail) {
            l.head.symbolRemoved(sym, this);
        }
    }

    /** Enter symbol sym in this scope if not already there.
     */
    public void enterIfAbsent(Symbol sym) {
        Assert.check(shared == 0);
        Entry e = lookup(sym.name);
        while (e.scope == this && e.sym.kind != sym.kind) e = e.next();
        if (e.scope != this) enter(sym);
    }

    /** Given a class, is there already a class with same fully
     *  qualified name in this (import) scope?
     */
    public boolean includes(Symbol c) {
        for (Scope.Entry e = lookup(c.name);
             e.scope == this;
             e = e.next()) {
            if (e.sym == c) return true;
        }
        return false;
    }

    static final Filter<Symbol> noFilter = new Filter<Symbol>() {
        public boolean accepts(Symbol s) {
            return true;
        }
    };

    /**
     * 查找相关的符号<p>
     * Return the entry associated with given name, starting in
     *  this scope and proceeding outwards. If no entry was found,
     *  return the sentinel, which is characterized by having a null in
     *  both its scope and sym fields, whereas both fields are non-null
     *  for regular entries.
     */
    public Entry lookup(Name name) {
        return lookup(name, noFilter);
    }
    public Entry lookup(Name name, Filter<Symbol> sf) {
        Entry e = table[getIndex(name)];
        if (e == null || e == sentinel)
            return sentinel;
        while (e.scope != null && (e.sym.name != name || !sf.accepts(e.sym)))
            e = e.shadowed;
        return e;
    }

    /*void dump (java.io.PrintStream out) {
        out.println(this);
        for (int l=0; l < table.length; l++) {
            Entry le = table[l];
            out.print("#"+l+": ");
            if (le==sentinel) out.println("sentinel");
            else if(le == null) out.println("null");
            else out.println(""+le+" s:"+le.sym);
        }
    }*/

    /** Look for slot in the table.
     *  We use open addressing with double hashing.
     */
    int getIndex (Name name) {
        int h = name.hashCode();
        int i = h & hashMask;
        // The expression below is always odd, so it is guaranteed
        // to be mutually prime with table.length, a power of 2.
        int x = hashMask - ((h + (h >> 16)) << 1);
        int d = -1; // Index of a deleted item.
        for (;;) {
            Entry e = table[i];
            if (e == null)
                return d >= 0 ? d : i;
            if (e == sentinel) {
                // We have to keep searching even if we see a deleted item.
                // However, remember the index in case we fail to find the name.
                if (d < 0)
                    d = i;
            } else if (e.sym.name == name)
                return i;
            i = (i + x) & hashMask;
        }
    }

    public Iterable<Symbol> getElements() {
        return getElements(noFilter);
    }

    public Iterable<Symbol> getElements(final Filter<Symbol> sf) {
        return new Iterable<Symbol>() {
            public Iterator<Symbol> iterator() {
                return new Iterator<Symbol>() {
                    private Scope currScope = Scope.this;
                    private Scope.Entry currEntry = elems;
                    {
                        update();
                    }

                    public boolean hasNext() {
                        return currEntry != null;
                    }

                    public Symbol next() {
                        Symbol sym = (currEntry == null ? null : currEntry.sym);
                        if (currEntry != null) {
                            currEntry = currEntry.sibling;
                        }
                        update();
                        return sym;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    private void update() {
                        skipToNextMatchingEntry();
                        while (currEntry == null && currScope.next != null) {
                            currScope = currScope.next;
                            currEntry = currScope.elems;
                            skipToNextMatchingEntry();
                        }
                    }

                    void skipToNextMatchingEntry() {
                        while (currEntry != null && !sf.accepts(currEntry.sym)) {
                            currEntry = currEntry.sibling;
                        }
                    }
                };
            }
        };
    }

    public Iterable<Symbol> getElementsByName(Name name) {
        return getElementsByName(name, noFilter);
    }

    public Iterable<Symbol> getElementsByName(final Name name, final Filter<Symbol> sf) {
        return new Iterable<Symbol>() {
            @Override
            public Iterator<Symbol> iterator() {
                return new Iterator<Symbol>() {
                    Scope.Entry currentEntry = lookup(name, sf);

                    @Override
                    public boolean hasNext() {
                        return currentEntry.scope != null;
                    }

                    @Override
                    public Symbol next() {
                        Scope.Entry prevEntry = currentEntry;
                        currentEntry = currentEntry.next(sf);
                        return prevEntry.sym;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Scope[");
        for (Scope s = this; s != null ; s = s.next) {
            if (s != this) result.append(" | ");
            for (Entry e = s.elems; e != null; e = e.sibling) {
                if (e != s.elems) result.append(", ");
                result.append(e.sym);
            }
        }
        result.append("]");
        return result.toString();
    }

    /**
     * sym保存的就是这个Entry对象所封装的符号；scope保存sym所属的作用域；sibling用来指向单链表的下一个节点；shadowed用来解决冲突。
     * 在分析语句及表达式时，会频繁通过table数组查找符号，为了提高查询的效率，需要一个高效的组织方式。Javac采用了哈希表来存储符号，当产生冲突时，
     * 通过单链表与二次冲突检测法来解决冲突。一般，当符号的名称不同时，可以通过二次冲突检测法来避免冲突，也就是查找数组中另外一个可用的下标；
     * 如果符号的名称相同，则通过单链表来避免冲突。shadowed指向单链表中的下一个元素，这样就可以支持Java中相同名称的多重定义，对于Java来说，
     * 甚至可以在同一个作用域内使用相同名称定义不同的成员。<p>
     * A class for scope entries.
     */
    public static class Entry {

        /** The referenced symbol.
         *  sym == null   iff   this == sentinel
         */
        public Symbol sym;

        /** An entry with the same hash code, or sentinel.
         */
        private Entry shadowed;

        /** Next entry in same scope.
         */
        public Entry sibling;

        /** The entry's scope.
         *  scope == null   iff   this == sentinel
         *  for an entry in an import scope, this is the scope
         *  where the entry came from (i.e. was imported from).
         */
        public Scope scope;

        public Entry(Symbol sym, Entry shadowed, Entry sibling, Scope scope) {
            this.sym = sym;
            this.shadowed = shadowed;
            this.sibling = sibling;
            this.scope = scope;
        }

        /** Return next entry with the same name as this entry, proceeding
         *  outwards if not found in this scope.
         */
        public Entry next() {
            return shadowed;
        }

        public Entry next(Filter<Symbol> sf) {
            if (shadowed.sym == null || sf.accepts(shadowed.sym)) return shadowed;
            else return shadowed.next(sf);
        }

        public Scope getOrigin() {
            // The origin is only recorded for import scopes.  For all
            // other scope entries, the "enclosing" type is available
            // from other sources.  See Attr.visitSelect and
            // Attr.visitIdent.  Rather than throwing an assertion
            // error, we return scope which will be the same as origin
            // in many cases.
            return scope;
        }
    }

    public static class ImportScope extends Scope {

        public ImportScope(Symbol owner) {
            super(owner);
        }

        @Override
        Entry makeEntry(Symbol sym, Entry shadowed, Entry sibling, Scope scope, Scope origin) {
            return new ImportEntry(sym, shadowed, sibling, scope, origin);
        }

        static class ImportEntry extends Entry {
            private Scope origin;

            ImportEntry(Symbol sym, Entry shadowed, Entry sibling, Scope scope, Scope origin) {
                super(sym, shadowed, sibling, scope);
                this.origin = origin;
            }

            @Override
            public Scope getOrigin() { return origin; }
        }
    }

    public static class StarImportScope extends ImportScope implements ScopeListener {

        public StarImportScope(Symbol owner) {
            super(owner);
        }

        public void importAll (Scope fromScope) {
            for (Scope.Entry e = fromScope.elems; e != null; e = e.sibling) {
                if (e.sym.kind == Kinds.TYP && !includes(e.sym))
                    enter(e.sym, fromScope);
            }
            // Register to be notified when imported items are removed
            fromScope.addScopeListener(this);
        }

        public void symbolRemoved(Symbol sym, Scope s) {
            remove(sym);
        }
        public void symbolAdded(Symbol sym, Scope s) { }
    }

    /** An empty scope, into which you can't place anything.  Used for
     *  the scope for a variable initializer.
     */
    public static class DelegatedScope extends Scope {
        Scope delegatee;
        public static final Entry[] emptyTable = new Entry[0];

        public DelegatedScope(Scope outer) {
            super(outer, outer.owner, emptyTable);
            delegatee = outer;
        }
        public Scope dup() {
            return new DelegatedScope(next);
        }
        public Scope dupUnshared() {
            return new DelegatedScope(next);
        }
        public Scope leave() {
            return next;
        }
        public void enter(Symbol sym) {
            // only anonymous classes could be put here
        }
        public void enter(Symbol sym, Scope s) {
            // only anonymous classes could be put here
        }
        public void remove(Symbol sym) {
            throw new AssertionError(sym);
        }
        public Entry lookup(Name name) {
            return delegatee.lookup(name);
        }
    }

    /** A class scope adds capabilities to keep track of changes in related
     *  class scopes - this allows client to realize whether a class scope
     *  has changed, either directly (because a new member has been added/removed
     *  to this scope) or indirectly (i.e. because a new member has been
     *  added/removed into a supertype scope)
     */
    public static class CompoundScope extends Scope implements ScopeListener {

        public static final Entry[] emptyTable = new Entry[0];

        private List<Scope> subScopes = List.nil();
        private int mark = 0;

        public CompoundScope(Symbol owner) {
            super(null, owner, emptyTable);
        }

        public void addSubScope(Scope that) {
           if (that != null) {
                subScopes = subScopes.prepend(that);
                that.addScopeListener(this);
                mark++;
                for (ScopeListener sl : listeners) {
                    sl.symbolAdded(null, this); //propagate upwards in case of nested CompoundScopes
                }
           }
         }

        public void symbolAdded(Symbol sym, Scope s) {
            mark++;
            for (ScopeListener sl : listeners) {
                sl.symbolAdded(sym, s);
            }
        }

        public void symbolRemoved(Symbol sym, Scope s) {
            mark++;
            for (ScopeListener sl : listeners) {
                sl.symbolRemoved(sym, s);
            }
        }

        public int getMark() {
            return mark;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("CompoundScope{");
            String sep = "";
            for (Scope s : subScopes) {
                buf.append(sep);
                buf.append(s);
                sep = ",";
            }
            buf.append("}");
            return buf.toString();
        }

        @Override
        public Iterable<Symbol> getElements(final Filter<Symbol> sf) {
            return new Iterable<Symbol>() {
                public Iterator<Symbol> iterator() {
                    return new CompoundScopeIterator(subScopes) {
                        Iterator<Symbol> nextIterator(Scope s) {
                            return s.getElements(sf).iterator();
                        }
                    };
                }
            };
        }

        @Override
        public Iterable<Symbol> getElementsByName(final Name name, final Filter<Symbol> sf) {
            return new Iterable<Symbol>() {
                public Iterator<Symbol> iterator() {
                    return new CompoundScopeIterator(subScopes) {
                        Iterator<Symbol> nextIterator(Scope s) {
                            return s.getElementsByName(name, sf).iterator();
                        }
                    };
                }
            };
        }

        abstract class CompoundScopeIterator implements Iterator<Symbol> {

            private Iterator<Symbol> currentIterator;
            private List<Scope> scopesToScan;

            public CompoundScopeIterator(List<Scope> scopesToScan) {
                this.scopesToScan = scopesToScan;
                update();
            }

            abstract Iterator<Symbol> nextIterator(Scope s);

            public boolean hasNext() {
                return currentIterator != null;
            }

            public Symbol next() {
                Symbol sym = currentIterator.next();
                if (!currentIterator.hasNext()) {
                    update();
                }
                return sym;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private void update() {
                while (scopesToScan.nonEmpty()) {
                    currentIterator = nextIterator(scopesToScan.head);
                    scopesToScan = scopesToScan.tail;
                    if (currentIterator.hasNext()) return;
                }
                currentIterator = null;
            }
        }

        @Override
        public Entry lookup(Name name, Filter<Symbol> sf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Scope dup(Symbol newOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enter(Symbol sym, Scope s, Scope origin) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(Symbol sym) {
            throw new UnsupportedOperationException();
        }
    }

    /** An error scope, for which the owner should be an error symbol. */
    public static class ErrorScope extends Scope {
        ErrorScope(Scope next, Symbol errSymbol, Entry[] table) {
            super(next, /*owner=*/errSymbol, table);
        }
        public ErrorScope(Symbol errSymbol) {
            super(errSymbol);
        }
        public Scope dup() {
            return new ErrorScope(this, owner, table);
        }
        public Scope dupUnshared() {
            return new ErrorScope(this, owner, table.clone());
        }
        public Entry lookup(Name name) {
            Entry e = super.lookup(name);
            if (e.scope == null)
                return new Entry(owner, null, null, null);
            else
                return e;
        }
    }
}
