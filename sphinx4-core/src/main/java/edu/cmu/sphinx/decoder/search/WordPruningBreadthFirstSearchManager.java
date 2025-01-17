/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.decoder.search;

// a test search manager.

import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.*;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.*;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Provides the breadth first search. To perform recognition an application
 * should call initialize before recognition begins, and repeatedly call
 * <code> recognize </code> until Result.isFinal() returns true. Once a final
 * result has been obtained, <code> stopRecognition </code> should be called.
 * <p>
 * All scores and probabilities are maintained in the log math log domain.
 */

public class WordPruningBreadthFirstSearchManager extends TokenSearchManager {

    /**
     * The property that defines the name of the linguist to be used by this
     * search manager.
     */
    @S4Component(type = Linguist.class)
    public final static String PROP_LINGUIST = "linguist";

    /**
     * The property that defines the name of the linguist to be used by this
     * search manager.
     */
    @S4Component(type = Pruner.class)
    public final static String PROP_PRUNER = "pruner";

    /**
     * The property that defines the name of the scorer to be used by this
     * search manager.
     */
    @S4Component(type = AcousticScorer.class)
    public final static String PROP_SCORER = "scorer";

    /**
     * The property than, when set to <code>true</code> will cause the
     * recognizer to count up all the tokens in the active list after every
     * frame.
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_SHOW_TOKEN_COUNT = "showTokenCount";

    /**
     * The property that controls the number of frames processed for every time
     * the decode growth step is skipped. Setting this property to zero disables
     * grow skipping. Setting this number to a small integer will increase the
     * speed of the decoder but will also decrease its accuracy. The higher the
     * number, the less often the grow code is skipped. Values like 6-8 is known
     * to be the good enough for large vocabulary tasks. That means that one of
     * 6 frames will be skipped.
     */
    @S4Integer(defaultValue = 0)
    public final static String PROP_GROW_SKIP_INTERVAL = "growSkipInterval";

    /** The property that defines the type of active list to use */
    @S4Component(type = ActiveListManager.class)
    public final static String PROP_ACTIVE_LIST_MANAGER = "activeListManager";

    /** The property for checking if the order of states is valid. */
    @S4Boolean(defaultValue = false)
    public final static String PROP_CHECK_STATE_ORDER = "checkStateOrder";

    /** The property that specifies the maximum lattice edges */
    @S4Integer(defaultValue = 100)
    public final static String PROP_MAX_LATTICE_EDGES = "maxLatticeEdges";

    /**
     * The property that controls the amount of simple acoustic lookahead
     * performed. Setting the property to zero (the default) disables simple
     * acoustic lookahead. The lookahead need not be an integer.
     */
    @S4Double(defaultValue = 0)
    public final static String PROP_ACOUSTIC_LOOKAHEAD_FRAMES = "acousticLookaheadFrames";

    /** The property that specifies the relative beam width */
    @S4Double(defaultValue = 0.0)
    // TODO: this should be a more meaningful default e.g. the common 1E-80
    public final static String PROP_RELATIVE_BEAM_WIDTH = "relativeBeamWidth";


    private static final int DEFAULT_BESTTOKENMAP_SIZE = 2048;

    // -----------------------------------
    // Configured Subcomponents
    // -----------------------------------
    protected Linguist linguist; // Provides grammar/language info
    public Linguist linguist() { return linguist; }

    protected Pruner pruner; // used to prune the active list
    public Pruner pruner() { return pruner; }

    protected AcousticScorer scorer; // used to score the active list
    public AcousticScorer scorer() { return scorer; }

    protected ActiveListManager activeListManager;


    // -----------------------------------
    // Configuration data
    // -----------------------------------
    protected Logger logger;
    protected boolean showTokenCount;
    protected boolean checkStateOrder;
    private int growSkipInterval;
    protected float relativeBeamWidth;
    protected float acousticLookaheadFrames;
    private int maxLatticeEdges;

    // -----------------------------------
    // Instrumentation
    // -----------------------------------
    protected edu.cmu.sphinx.util.Timer scoreTimer;
    protected edu.cmu.sphinx.util.Timer pruneTimer;
    protected Timer growTimer;
    protected StatisticsVariable totalTokensScored;
    protected StatisticsVariable curTokensScored;
    protected StatisticsVariable tokensCreated;
    private long tokenSum;
    private int tokenCount;

    // -----------------------------------
    // Working data
    // -----------------------------------
    protected int currentFrameNumber; // the current frame number
    protected long currentCollectTime; // the current frame number
    protected ActiveList activeList; // the list of active tokens
    protected final List<Token> resultList = new FastList(); // the current set of results
    protected final Map<SearchState, Token> bestTokens = new ConcurrentHashMap<>(DEFAULT_BESTTOKENMAP_SIZE);//, 0.3F);;
    protected AlternateHypothesisManager loserManager;
    private int numStateOrder;
    // private TokenTracker tokenTracker;
    // private TokenTypeTracker tokenTypeTracker;
    protected boolean streamEnd;

    /**
     * Creates a pruning manager withs separate lists for tokens
     * @param linguist a linguist for search space
     * @param pruner pruner to drop tokens
     * @param scorer scorer to estimate token probability
     * @param activeListManager active list manager to store tokens
     * @param showTokenCount show count during decoding
     * @param relativeWordBeamWidth relative beam for lookahead pruning
     * @param growSkipInterval skip interval for grown
     * @param checkStateOrder check order of states during growth
     * @param buildWordLattice build a lattice during decoding
     * @param maxLatticeEdges max edges to keep in lattice
     * @param acousticLookaheadFrames frames to do lookahead
     * @param keepAllTokens keep tokens including emitting tokens
     */
    public WordPruningBreadthFirstSearchManager(Linguist linguist, Pruner pruner, AcousticScorer scorer,
                                         ActiveListManager activeListManager, boolean showTokenCount, double relativeWordBeamWidth, int growSkipInterval,
                                         boolean checkStateOrder, boolean buildWordLattice, int maxLatticeEdges, float acousticLookaheadFrames,
                                         boolean keepAllTokens) {

        this.logger = Logger.getLogger(getClass().getName());
        
        this.linguist = linguist;
        this.pruner = pruner;
        this.scorer = scorer;
        this.activeListManager = activeListManager;
        this.showTokenCount = showTokenCount;
        this.growSkipInterval = growSkipInterval;
        this.checkStateOrder = checkStateOrder;
        this.buildWordLattice = buildWordLattice;
        this.maxLatticeEdges = maxLatticeEdges;
        this.acousticLookaheadFrames = acousticLookaheadFrames;
        this.keepAllTokens = keepAllTokens;

        this.relativeBeamWidth = LogMath.linearToLog(relativeWordBeamWidth);
    }

    WordPruningBreadthFirstSearchManager() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
     * .props.PropertySheet)
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        logger = ps.getLogger();

        linguist = (Linguist) ps.getComponent(PROP_LINGUIST);
        pruner = (Pruner) ps.getComponent(PROP_PRUNER);
        scorer = (AcousticScorer) ps.getComponent(PROP_SCORER);
        activeListManager = (ActiveListManager) ps.getComponent(PROP_ACTIVE_LIST_MANAGER);
        showTokenCount = ps.getBoolean(PROP_SHOW_TOKEN_COUNT);
        growSkipInterval = ps.getInt(PROP_GROW_SKIP_INTERVAL);

        checkStateOrder = ps.getBoolean(PROP_CHECK_STATE_ORDER);
        maxLatticeEdges = ps.getInt(PROP_MAX_LATTICE_EDGES);
        acousticLookaheadFrames = ps.getFloat(PROP_ACOUSTIC_LOOKAHEAD_FRAMES);

        relativeBeamWidth = LogMath.linearToLog(ps.getDouble(PROP_RELATIVE_BEAM_WIDTH));
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.decoder.search.SearchManager#allocate()
     */
    public void allocate() {
        // tokenTracker = new TokenTracker();
        // tokenTypeTracker = new TokenTypeTracker();

        scoreTimer = TimerPool.getTimer(this, "Score");
        pruneTimer = TimerPool.getTimer(this, "Prune");
        growTimer = TimerPool.getTimer(this, "Grow");

        totalTokensScored = StatisticsVariable.the("totalTokensScored");
        curTokensScored = StatisticsVariable.the("curTokensScored");
        tokensCreated = StatisticsVariable.the("tokensCreated");

        try {
//            Stream.of(linguist, pruner, scorer).parallel().forEach((Configurable x) -> {
//                try {
//                    x.allocate();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
            linguist.allocate();
            pruner.allocate();
            scorer.allocate();
        } catch (IOException e) {
            throw new RuntimeException("Allocation of search manager resources failed", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.decoder.search.SearchManager#deallocate()
     */
    public void deallocate() {
        try {
            scorer.deallocate();
            pruner.deallocate();
            linguist.deallocate();
        } catch (IOException e) {
            throw new RuntimeException("Deallocation of search manager resources failed", e);
        }
    }

    /**
     * Called at the start of recognition. Gets the search manager ready to
     * recognize
     */
    public void startRecognition() {
        linguist.startRecognition();
        pruner.startRecognition();
        scorer.startRecognition();
        localStart();
    }

    /**
     * Performs the recognition for the given number of frames.
     * 
     * @param nFrames
     *            the number of frames to recognize
     * @return the current result
     */
    public Result recognize(int nFrames) {
        boolean done = false;
        Result result = null;
        streamEnd = false;

        for (int i = 0; i < nFrames && !done; i++) {
            done = recognize();
        }

        if (!streamEnd) {
            result = new Result(loserManager, activeList, resultList, currentCollectTime, done, linguist.getSearchGraph()
                    .getWordTokenFirst(), true);
        }

        // tokenTypeTracker.show();
        if (showTokenCount) {
            showTokenCount();
        }
        return result;
    }

    protected boolean recognize() {

        activeList = activeListManager.getEmittingList();
        boolean more = scoreTokens();

        if (more) {
            pruneBranches();
            currentFrameNumber++;
            if (growSkipInterval == 0 || (currentFrameNumber % growSkipInterval) != 0) {
                clearCollectors();
                growEmittingBranches();
                growNonEmittingBranches();
            }
        }
        return !more;
    }

    /**
     * Clears lists and maps before next expansion stage
     */
    private void clearCollectors() {
        resultList.clear();// = /*Collections.synchronizedList*/( new LinkedList<>() );
        bestTokens.clear(); //=
                //new ConcurrentHashMap<>(DEFAULT_BESTTOKENMAP_SIZE);//, 0.3F);
                //new ConcurrentHashMapUnsafe<>(DEFAULT_BESTTOKENMAP_SIZE);//, 0.3F);
                //new UnifiedMap<>(DEFAULT_BESTTOKENMAP_SIZE);

        activeListManager.clearEmittingList();
    }

    /** Terminates a recognition */
    public void stopRecognition() {

        localStop();
        scorer.stopRecognition();
        pruner.stopRecognition();
        linguist.stopRecognition();
    }

    /**
     * Gets the initial grammar node from the linguist and creates a
     * GrammarNodeToken
     */
    protected void localStart() {
        SearchGraph searchGraph = linguist.getSearchGraph();
        currentFrameNumber = 0;
        curTokensScored.value = 0;
        numStateOrder = searchGraph.getNumStateOrder();
        activeListManager.setNumStateOrder(numStateOrder);
        if (buildWordLattice) {
            loserManager = new AlternateHypothesisManager(maxLatticeEdges);
        }

        activeList = activeListManager.getEmittingList();
        activeList.add(new Token(searchGraph.getInitialState(), -1));

        clearCollectors();

        growBranches();
        growNonEmittingBranches();
        // tokenTracker.setEnabled(false);
        // tokenTracker.startUtterance();
    }

    /** Local cleanup for this search manager */
    protected void localStop() {
        // tokenTracker.stopUtterance();
    }

    /**
     * Goes through the active list of tokens and expands each token, finding
     * the set of successor tokens until all the successor tokens are emitting
     * tokens.
     */
    protected void growBranches() {
        growTimer.start();
        //float relativeBeamThreshold = activeList.getBeamThreshold();
//        if (logger.isLoggable(Level.FINE)) {
//            logger.fine("Frame: " + currentFrameNumber + " thresh : " + relativeBeamThreshold + " bs "
//                    + activeList.getBestScore() + " tok " + activeList.getBestToken());
//        }

        //activeList.
        activeList.forWhile(t -> {
                    if (t.score() >= activeList.getBeamThreshold() /* this value may increase as this loop progresses */) {
                        int added = collectSuccessorTokens(t);
                        //System.out.println(t.score() + " " + t.getWord() + " \t added=" + added);
                        return true;
                    } else {
                        //since the list is sorted, everything after this will also be below threshold
                        return false;
                    }

                });
        growTimer.stop();
    }

    /**
     * Grows the emitting branches. This version applies a simple acoustic
     * lookahead based upon the rate of change in the current acoustic score.
     */
    protected void growEmittingBranches() {
        if (acousticLookaheadFrames <= 0.0f) {
            growBranches();
            return;
        }

        growTimer.start();
        final float[] bestScore = {-Float.MAX_VALUE};

        activeList.forEach( (Token t) -> {
            float score = t.score() + t.getAcousticScore() * acousticLookaheadFrames;
            if (score > bestScore[0]) {
                bestScore[0] = score;
            }
        });

        float relativeBeamThreshold = bestScore[0] + relativeBeamWidth;


        activeList.forEach( (Token t) -> {
            if (t.score() + t.getAcousticScore() * acousticLookaheadFrames > relativeBeamThreshold)
                collectSuccessorTokens(t);
        });

//        for (Token t : activeList) {
//        }
        growTimer.stop();
    }

    /**
     * Grow the non-emitting branches, until the tokens reach an emitting state.
     */
    private void growNonEmittingBranches() {
        for (Iterator<ActiveList> i = activeListManager.getNonEmittingListIterator(); i.hasNext();) {
            activeList = i.next();
            //if (activeList != null) {
                i.remove();
                pruneBranches();
                growBranches();
            //}
        }
    }

    /**
     * Calculate the acoustic scores for the active list. The active list should
     * contain only emitting tokens.
     * 
     * @return <code>true</code> if there are more frames to score, otherwise,
     *         false
     */
    protected boolean scoreTokens() {
        boolean moreTokens;

        scoreTimer.start();
        Data data = scorer.calculateScores(activeList);
        scoreTimer.stop();


        Token bestToken = null;
        if (data instanceof Token) {
            bestToken = (Token) data;
        } else if (data == null) {
            streamEnd = true;
        }

        if (bestToken != null) {
            currentCollectTime = bestToken.getCollectTime();
        }
        
        moreTokens = (bestToken != null);

        // monitorWords(activeList);
        monitorStates(activeList);

        // System.out.println("BEST " + bestToken);

        int s = activeList.size();
        curTokensScored.value += s;
        totalTokensScored.value += s;

        return moreTokens;
    }

    /**
     * Keeps track of and reports all of the active word histories for the given
     * active list
     * 
     * @param activeList
     *            the active list to track
     */
    @SuppressWarnings("unused")
    private void monitorWords(ActiveList activeList) {

        // WordTracker tracker1 = new WordTracker(currentFrameNumber);
        //
        // for (Token t : activeList) {
        // tracker1.add(t);
        // }
        // tracker1.dump();
        //
        // TokenTracker tracker2 = new TokenTracker();
        //
        // for (Token t : activeList) {
        // tracker2.add(t);
        // }
        // tracker2.dumpSummary();
        // tracker2.dumpDetails();
        //
        // TokenTypeTracker tracker3 = new TokenTypeTracker();
        //
        // for (Token t : activeList) {
        // tracker3.add(t);
        // }
        // tracker3.dump();

        // StateHistoryTracker tracker4 = new
        // StateHistoryTracker(currentFrameNumber);

        // for (Token t : activeList) {
        // tracker4.add(t);
        // }
        // tracker4.dump();
    }

    /**
     * Keeps track of and reports statistics about the number of active states
     * 
     * @param activeList
     *            the active list of states
     */
    protected void monitorStates(ActiveList activeList) {

        tokenSum += activeList.size();
        tokenCount++;

//        if ((tokenCount % 1000) == 0) {
//            logger.info("Average Tokens/State: " + (tokenSum / tokenCount));
//        }
    }

    /** Removes unpromising branches from the active list */
    protected void pruneBranches() {
        pruneTimer.start();
        activeList = activeList.commit();
        pruneTimer.stop();
    }

    /**
     * Checks that the given two states are in legitimate order.
     * 
     * @param fromState parent state
     * @param toState child state
     */
    protected void checkStateOrder(SearchState fromState, SearchState toState) {
        if (fromState.getOrder() == numStateOrder - 1) {
            return;
        }

        if (fromState.getOrder() > toState.getOrder()) {
            throw new Error("IllegalState order: from " + fromState.getClass().getName() + ' ' + fromState.toPrettyString()
                    + " order: " + fromState.getOrder() + " to " + toState.getClass().getName() + ' ' + toState.toPrettyString()
                    + " order: " + toState.getOrder());
        }
    }

    /**
     * Collects the next set of emitting tokens from a token and accumulates
     * them in the active or result lists
     *
     * @param token
     *            the token to collect successors from be immediately expanded
     *            are placed. Null if we should always expand all nodes.
     */
    protected int collectSuccessorTokens(Token token) {

        // tokenTracker.add(token);
        // tokenTypeTracker.add(token);

        // If this is a final state, add it to the final list

        if (token.isFinal()) {
            resultList.add(getResultListPredecessor(token));
            return 0;
        }

        // if this is a non-emitting token and we've already
        // visited the same state during this frame, then we
        // are in a grammar loop, so we don't continue to expand.
        // This check only works properly if we have kept all of the
        // tokens (instead of skipping the non-word tokens).
        // Note that certain linguists will never generate grammar loops
        // (lextree linguist for example). For these cases, it is perfectly
        // fine to disable this check by setting keepAllTokens to false

        if (!token.isEmitting() && (keepAllTokens && isVisited(token))) {
            return 0;
        }

        return expandSuccessorTokens(token);
    }

    private int expandSuccessorTokens(Token token) {
        Token predecessor = getResultListPredecessor(token);

        // For each successor
        // calculate the entry score for the token based upon the
        // predecessor token score and the transition probabilities
        // if the score is better than the best score encountered for
        // the SearchState and frame then create a new token, add
        // it to the lattice and the SearchState.
        // If the token is an emitting token add it to the list,
        // otherwise recursively collect the new tokens successors.

        SearchState state = token.getSearchState();
        SearchStateArc[] arcs = state.getSuccessors();
        int added = 0;
        for (SearchStateArc arc : arcs) {
            SearchState nextState = arc.getState();

            if (checkStateOrder) {
                checkStateOrder(state, nextState);
            }

            // We're actually multiplying the variables, but since
            // these come in log(), multiply gets converted to add
            float logEntryScore = token.score() + arc.getProbability();

            Token bestToken = bestTokens.get(nextState);

            if (bestToken == null) {
                Token newBestToken = new Token(predecessor, nextState, logEntryScore, arc.getInsertionProbability(),
                        arc.getLanguageProbability(), currentCollectTime);
                if (activeListManager.add(newBestToken)) {
                    tokensCreated.value++;
                    added++;
                    bestTokens.putIfAbsent(nextState, newBestToken);
                }

            } else if (bestToken.score() < logEntryScore) {
                // System.out.println("Updating " + bestToken + " with " +
                // newBestToken);
                Token oldPredecessor = bestToken.predecessor();
                bestToken.update(predecessor, logEntryScore, arc.getInsertionProbability(),
                        arc.getLanguageProbability(), currentCollectTime);
                if (buildWordLattice && nextState instanceof WordSearchState) {
                    loserManager.addAlternatePredecessor(bestToken, oldPredecessor);
                }
            } else if (buildWordLattice && nextState instanceof WordSearchState) {
                if (predecessor != null) {
                    loserManager.addAlternatePredecessor(bestToken, predecessor);
                }
            }
        }
        return added;
    }

    /**
     * Determines whether or not we've visited the state associated with this
     * token since the previous frame.
     * 
     * @param t token to check
     * @return true if we've visited the search state since the last frame
     */
    protected static boolean isVisited(Token t) {
        SearchState curState = t.getSearchState();

        t = t.predecessor();

        while (t != null && !t.isEmitting()) {
            if (curState.equals(t.getSearchState())) {
                //System.out.println("CS " + curState + " match " + t.getSearchState());
                return true;
            }
            t = t.predecessor();
        }
        return false;
    }

//    /**
//     * Determine if the given token should be expanded
//     *
//     * @param t
//     *            the token to test
//     * @return <code>true</code> if the token should be expanded
//     */
//    protected static boolean allowExpansion(Token t) {
//        return true; // currently disabled
//    }

    /**
     * Counts all the tokens in the active list (and displays them). This is an
     * expensive operation.
     */
    protected void showTokenCount() {
        Set<Token> tokenSet = new HashSet<>();

        for (Token token : activeList) {
            while (token != null) {
                tokenSet.add(token);
                token = token.predecessor();
            }
        }

        System.out.println("Token Lattice size: " + tokenSet.size());

        tokenSet = new HashSet<>();

        for (Token token : resultList) {
            while (token != null) {
                tokenSet.add(token);
                token = token.predecessor();
            }
        }

        System.out.println("Result Lattice size: " + tokenSet.size());
    }

//    /**
//     * Returns the ActiveList.
//     *
//     * @return the ActiveList
//     */
//    public ActiveList getActiveList() {
//        return activeList;
//    }

//    /**
//     * Sets the ActiveList.
//     *
//     * @param activeList
//     *            the new ActiveList
//     */
//    public void setActiveList(ActiveList activeList) {
//        this.activeList = activeList;
//    }

//    /**
//     * Returns the result list.
//     *
//     * @return the result list
//     */
//    public List<Token> getResultList() {
//        return resultList;
//    }

//    /**
//     * Sets the result list.
//     *
//     * @param resultList
//     *            the new result list
//     */
//    public void setResultList(List<Token> resultList) {
//        this.resultList = resultList;
//    }
//
//    /**
//     * Returns the current frame number.
//     *
//     * @return the current frame number
//     */
//    public int getCurrentFrameNumber() {
//        return currentFrameNumber;
//    }
//
////    /**
////     * Returns the Timer for growing.
////     *
////     * @return the Timer for growing
////     */
////    public Timer getGrowTimer() {
////        return growTimer;
////    }
//
//    /**
//     * Returns the tokensCreated StatisticsVariable.
//     *
//     * @return the tokensCreated StatisticsVariable.
//     */
//    public StatisticsVariable getTokensCreated() {
//        return tokensCreated;
//    }

}
