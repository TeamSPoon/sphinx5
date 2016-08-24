package edu.cmu.sphinx.decoder.search;

import org.eclipse.collections.impl.collector.Collectors2;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortingActiveList implements ActiveList {

    private SortingActiveListFactory sortingActiveListFactory;
    protected final int absoluteBeamWidth;
    protected final float logRelativeBeamWidth;


    //private Token bestToken;
    // when the list is changed these things should be
    // changed/updated as well
    //private List<Token> tokenList;

    private final NavigableSet<Token> tokens;
    //private final ArrayList buffer = new ArrayList(); //copy where the set is buffered for traversal
    private final AtomicInteger size = new AtomicInteger(0); //concurrent skiplist set is slow tracking its own size so it can be done here



    public SortingActiveList(boolean concurrent, int absoluteBeamWidth, float logRelativeBeamWidth) {
        if (concurrent) {
            tokens =
                    new ConcurrentSkipListSet<>();
                    //Collections.synchronizedNavigableSet(new TreeSet());
        } else {
            tokens = new TreeSet();
        }

        this.absoluteBeamWidth = absoluteBeamWidth;
        this.logRelativeBeamWidth = logRelativeBeamWidth;

    }
    /**
     * Creates an empty active list
     *
     * @param absoluteBeamWidth    beam for absolute pruning
     * @param logRelativeBeamWidth beam for relative pruning
     */
    public SortingActiveList(SortingActiveListFactory sortingActiveListFactory, int absoluteBeamWidth, float logRelativeBeamWidth) {
        this(true, absoluteBeamWidth, logRelativeBeamWidth);
        this.sortingActiveListFactory = sortingActiveListFactory;

        //int initListSize = absoluteBeamWidth > 0 ? absoluteBeamWidth : DEFAULT_SIZE;
        //this.tokens = new ArrayList<>(initListSize);
    }

    public SortingActiveList(boolean concurrent, ActiveList copyParamFrom) {
        this(concurrent, ((SortingActiveList)copyParamFrom).absoluteBeamWidth, ((SortingActiveList)copyParamFrom).logRelativeBeamWidth);
    }


    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public boolean add(Token token) {


        int nt = size.get();
        int capacity = absoluteBeamWidth;

        if (nt >= capacity) {
            if (token.score() <= tokens.last().score())
                return false; //reject immediately
        }

        int delta = 0;
        if (tokens.add(token)) {
            delta++;
            int toRemove = (nt + 1) - (capacity);
            for (int i = 0; i < toRemove; i++) {
                if (tokens.pollLast() != null) {
                    delta--;
                }
            }
            size.addAndGet(delta);
        }

        return true;
    }

    /**
     * Purges excess members. Reduce the size of the token list to the absoluteBeamWidth
     *
     * @return a (possible new) active list
     */
    public final ActiveList commit() {
        //this self-sorts, no further maintenance required
        return this;
    }


    /**
     * gets the beam threshold best upon the best scoring token
     *
     * @return the beam threshold
     */
    public final float getBeamThreshold() {
        return bestScore() + logRelativeBeamWidth;
    }


    /**
     * gets the best score in the list
     *
     * @return the best score
     */
    public final float bestScore() {
//            float bestScore = -Float.MAX_VALUE;
//            if (bestToken != null) {
//                bestScore = bestToken.score();
//            }
//            return bestScore;
        if (isEmpty())
            return -Float.MAX_VALUE;
        else
            return tokens.first().score();
    }

    @Override
    public float worstScore() {
        if (size.get() < absoluteBeamWidth)
            return -Float.MAX_VALUE;
        else
            return tokens.last().score();
    }

    private boolean isEmpty() {
        return size.get() <= 0;
    }


    /**
     * Gets the best scoring token for this active list
     *
     * @return the best scoring token
     */
    public Token best() {
        //return bestToken;
        return !isEmpty() ? tokens.first() : null;
    }


    /**
     * Retrieves the iterator for this tree.
     *
     * @return the iterator for this token list
     */
    public Iterator<Token> iterator() {
        return tokens.iterator();
    }

    @Override
    public void forEach(Consumer<? super Token> action) {
        //buffer.clear();
        //buffer.addAll(tokens);


        //this buffers the set into sublists and then parallel processes them in the fork join common pool
        Stream<Token> stream = tokens.stream().sequential();

        int loadsPerThread = 3; //workload granularity

        int chunkSize = Math.max(1, size.get() / (loadsPerThread * sortingActiveListFactory.threads));

        if (chunkSize > 1) {
            stream.collect(Collectors2.chunk(chunkSize)).parallelStream().forEach(x -> {
                //System.out.println(Thread.currentThread() + " thread processing " + x.size());
                x.forEach(action);
            });
        } else {
            //dont chunk just parallelize them all
            stream.collect(Collectors.toList()).parallelStream().forEach(action);
        }

    }


    /**
     * Returns the number of tokens on this active list
     *
     * @return the size of the active list
     */
    public final int size() {
        return size.get();
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.decoder.search.ActiveList#newInstance()
    */
    public ActiveList newInstance() {
        return sortingActiveListFactory.newInstance();
    }
}
