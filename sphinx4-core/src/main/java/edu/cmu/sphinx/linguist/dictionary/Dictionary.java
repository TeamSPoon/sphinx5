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
package edu.cmu.sphinx.linguist.dictionary;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

import java.io.IOException;

/**
 * Provides a generic interface to a dictionary. The dictionary is responsible for determining how a word is
 * pronounced.
 */
public interface Dictionary extends Configurable {

    /** Spelling of the sentence start word. */
    String SENTENCE_START_SPELLING = "<s>";
    /** Spelling of the sentence end word. */
    String SENTENCE_END_SPELLING = "</s>";
    /** Spelling of the 'word' that marks a silence */
    String SILENCE_SPELLING = "<sil>";

    /** The property for the dictionary file path. */
    @S4String
    String PROP_DICTIONARY = "dictionaryPath";

    /** The property for the g2p model file path. */
    @S4String(defaultValue = "")
    String PROP_G2P_MODEL_PATH = "g2pModelPath";

    /** The property for the g2p model file path. */
    @S4Integer(defaultValue = 1)
    String PROP_G2P_MAX_PRONUNCIATIONS = "g2pMaxPron";

    /** The property for the filler dictionary file path. */
    @S4String
    String PROP_FILLER_DICTIONARY = "fillerPath";
    /**
     * The property that specifies the word to substitute when a lookup fails to find the word in the
     * dictionary. If this is not set, no substitute is performed.
     */
    @S4String(mandatory = false)
    String PROP_WORD_REPLACEMENT = "wordReplacement";

    /** The property that defines the name of the unit manager that is used to convert strings to Unit objects */
    @S4Component(type = UnitManager.class, defaultClass = UnitManager.class)
    String PROP_UNIT_MANAGER = "unitManager";

    /**
     * The property for the custom dictionary file paths. This addenda property points to a possibly
     * empty list of URLs to dictionary addenda.  Each addendum should contain word pronunciations in the same Sphinx-3
     * dictionary format as the main dictionary.  Words in the addendum are added after the words in the main dictionary
     * and will override previously specified pronunciations.  If you wish to extend the set of pronunciations for a
     * particular word, add a new pronunciation by number.  For example, in the following addendum, given that the
     * aforementioned main dictionary is specified, the pronunciation for 'EIGHT' will be overridden by the addenda,
     * while the pronunciation for 'SIX' and 'ZERO' will be augmented and a new pronunciation for 'ELEVEN' will be
     * added.
     * <pre>
     *          EIGHT   OW T
     *          SIX(2)  Z IH K S
     *          ZERO(3)  Z IY Rl AH
     *          ELEVEN   EH L EH V AH N
     * </pre>
     */
    @S4String(mandatory = false)
    String PROP_ADDENDA = "addenda";

    /**
     * Returns a Word object based on the spelling and its classification. The behavior of this method is also affected
     * by the properties wordReplacement and g2pModel
     *
     * @param text the spelling of the word of interest.
     * @return a Word object
     * @see edu.cmu.sphinx.linguist.dictionary.Pronunciation
     */
    Word word(String text);


    /**
     * Returns the sentence start word.
     *
     * @return the sentence start word
     */
    Word getSentenceStartWord();


    /**
     * Returns the sentence end word.
     *
     * @return the sentence end word
     */
    Word getSentenceEndWord();


    /**
     * Returns the silence word.
     *
     * @return the silence word
     */
    Word getSilenceWord();

    /**
     * Gets the set of all filler words in the dictionary
     *
     * @return an array (possibly empty) of all filler words
     */
    Word[] getFillerWords();


    /**
     * Allocates the dictionary
     *
     * @throws IOException if there is trouble loading the dictionary
     */
    void allocate() throws IOException;


    /** Deallocates the dictionary */
    void deallocate();
}
