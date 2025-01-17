package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.acoustic.*;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneSequence;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AllphoneLinguist implements Linguist {

    /** The property that defines the acoustic model to use when building the search graph */
    @S4Component(type = AcousticModel.class)
    public final static String PROP_ACOUSTIC_MODEL = "acousticModel";
    
    /**
     * The property that controls phone insertion probability.
     * Default value for context independent phoneme decoding is 0.05,
     * while for context dependent - 0.01.
     */
    @S4Double(defaultValue = 0.05)
    public final static String PROP_PIP = "phoneInsertionProbability";
    
    /**
     * The property that controls whether to use context dependent phones.
     * Changing it for true, don't forget to tune phone insertion probability.
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_CD = "useContextDependentPhones";
    
    private AcousticModel acousticModel;
    private ArrayList<HMM> ciHMMs;
    private ArrayList<HMM> fillerHMMs;
    private ArrayList<HMM> leftContextSilHMMs;
    private HashMap<SenoneSequence, ArrayList<Unit>> senonesToUnits;
    private HashMap<Unit, HashMap<Unit, ArrayList<HMM>>> cdHMMs;
    private float pip;
    private boolean useCD;
    
    public AllphoneLinguist() {    
        
    }
    
    public void newProperties(PropertySheet ps) throws PropertyException {
        acousticModel = (AcousticModel) ps.getComponent(PROP_ACOUSTIC_MODEL);
        pip = LogMath.linearToLog(ps.getFloat(PROP_PIP));
        
        useCD = ps.getBoolean(PROP_CD);
        if (useCD)
            createContextDependentSuccessors();
        else
            createContextIndependentSuccessors();
    }

    public SearchGraph getSearchGraph() {
        return new AllphoneSearchGraph(this);
    }

    public void startRecognition() {
    }

    public void stopRecognition() {
    }

    public void allocate() {
    }

    public void deallocate() {
    }
    
    public AcousticModel getAcousticModel() {
        return acousticModel;
    }
    
    public float getPhoneInsertionProb() {
        return pip;
    }
    
    public boolean useContextDependentPhones() {
        return useCD;
    }
    
    public ArrayList<HMM> getCISuccessors() {
        return ciHMMs;
    }
    
    public ArrayList<HMM> getCDSuccessors(Unit lc, Unit base) {
        if (lc.filler)
            return leftContextSilHMMs;
        if (base == UnitManager.SILENCE)
            return fillerHMMs;
        return cdHMMs.get(lc).get(base);
    }

    public ArrayList<Unit> getUnits(SenoneSequence senoneSeq) {
        return senonesToUnits.get(senoneSeq);
    }

    private void createContextIndependentSuccessors() {
        Iterator<HMM> hmmIter = acousticModel.getHMMIterator();
        ciHMMs = new ArrayList<>();
        senonesToUnits = new HashMap<>();
        while (hmmIter.hasNext()) {
            HMM hmm = hmmIter.next();
            if (!hmm.getUnit().isContextDependent()) {
                ArrayList<Unit> sameSenonesUnits;
                SenoneSequence senoneSeq = ((SenoneHMM) hmm).senoneSequence;
                if ((sameSenonesUnits = senonesToUnits.get(senoneSeq)) == null) {
                    sameSenonesUnits = new ArrayList<>();
                    senonesToUnits.put(senoneSeq, sameSenonesUnits);
                }
                sameSenonesUnits.add(hmm.getUnit());
                ciHMMs.add(hmm);
            }
        }
    }
    
    private void createContextDependentSuccessors() {
        cdHMMs = new HashMap<>();
        senonesToUnits = new HashMap<>();
        fillerHMMs = new ArrayList<>();
        leftContextSilHMMs = new ArrayList<>();
        Iterator<HMM> hmmIter = acousticModel.getHMMIterator();
        while (hmmIter.hasNext()) {
            HMM hmm = hmmIter.next();
            ArrayList<Unit> sameSenonesUnits;
            SenoneSequence senoneSeq = ((SenoneHMM) hmm).senoneSequence;
            if ((sameSenonesUnits = senonesToUnits.get(senoneSeq)) == null) {
                sameSenonesUnits = new ArrayList<>();
                senonesToUnits.put(senoneSeq, sameSenonesUnits);
            }
            sameSenonesUnits.add(hmm.getUnit());
            if (hmm.getUnit().filler) {
                fillerHMMs.add(hmm);
                continue;
            }
            if (hmm.getUnit().isContextDependent()) {
                LeftRightContext context = (LeftRightContext) hmm.getUnit().context;
                Unit lc = context.left[0];
                if (lc == UnitManager.SILENCE) {
                    leftContextSilHMMs.add(hmm);
                    continue;
                }
                Unit base = hmm.getUnit().baseUnit;
                HashMap<Unit, ArrayList<HMM>> lcSuccessors; 
                if ((lcSuccessors = cdHMMs.get(lc)) == null) {
                    lcSuccessors = new HashMap<>();
                    cdHMMs.put(lc, lcSuccessors);
                }
                ArrayList<HMM> lcBaseSuccessors;
                if ((lcBaseSuccessors = lcSuccessors.get(base)) == null) {
                    lcBaseSuccessors = new ArrayList<>();
                    lcSuccessors.put(base, lcBaseSuccessors);
                }
                lcBaseSuccessors.add(hmm);
            }
        }
        leftContextSilHMMs.addAll(fillerHMMs);
    }
    
}
