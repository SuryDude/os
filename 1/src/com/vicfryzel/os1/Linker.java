// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os1;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Linker {
  protected Reader reader;
  protected StreamTokenizer tokenizer;
  protected Map<Integer, List<String>> modules;
  protected Map<String, Integer> symbolTable;
  protected Logger logger;
  protected static final int DEF_STATE = 0;
  protected static final int USE_STATE = 1;
  protected static final int PROG_STATE = 2;
  // Do not modify this value, it is required to be this
  protected static final int EXTERNAL_MODIFIER = 10000;
  // Do not modify this value, it is required to be this
  protected static final int RELATIVE_MODIFIER = 20000;

  /**
   * Create a new Linker based on the given Reader for input.
   *
   * @param r Reader wrapping input stream.
   */
  public Linker(Reader r) {
    logger = Logger.getLogger("Linker");
    logger.setLevel(Level.WARNING);
    reader = r;
    reset();
  }

  /**
   * Resets this linker, including read modules and symbol table.
   */
  protected void reset() {
    resetTokenizer();
    modules = new TreeMap<Integer, List<String>>();
    symbolTable = new TreeMap<String, Integer>();
  }

  /**
   * Sets the tokenizer back to the beginning of this Linker's Reader.
   */
  protected void resetTokenizer() {
    try {
      reader.reset();
    } catch (IOException e) {}
    tokenizer = new StreamTokenizer(reader);
    tokenizer.eolIsSignificant(false);
    tokenizer.parseNumbers();
  }

  /**
   * Gets modules as a list of tokens.  Although all modules are read here,
   * modules are processed one at a time.
   *
   * @return List of Ns, types, words that compose the input.
   */
  protected List<String> getModulesAsParts() {
    List<String> parts = new ArrayList<String>();
    try {
      int next;
      int state = 0;
      while ((next = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
        if (next == tokenizer.TT_NUMBER) {
          parts.add(String.valueOf(tokenizer.nval));
        } else if (next == tokenizer.TT_WORD) {
          parts.add(tokenizer.sval);
        }
      }
    } catch (IOException e) {}
    return parts;
  }

  /**
   * Pass 1 processes each module one at a time, calculating their base and
   * generating a symbol table.
   *
   * Symbol table and processed modules are stored in class members, pending
   * pass 2.
   */
  public void pass1() {
    reset();

    List<String> modulesAsParts = getModulesAsParts();
    List<String> module = new ArrayList<String>();
    int base = 0;
    int state = DEF_STATE;
    Map<String, Integer> localDefs = new HashMap<String, Integer>();
    for (Iterator<String> partIterator = modulesAsParts.iterator(); partIterator.hasNext();) {
      int n = 0;
      try {
        n = (int) Double.parseDouble(partIterator.next());
      } catch (NumberFormatException e) {
        // Error, no n
      }
      module.add(String.valueOf(n));

      for (int i = 0; i < n && partIterator.hasNext(); i++) {
        String type = partIterator.next();
        Integer word = null;
        try {
          word = (int) Double.parseDouble(partIterator.next());
        } catch (NumberFormatException e) {
          // Parse error, missing pair
        } catch (NoSuchElementException e) {
          // Same parse error, missing pair
        }
        if (state == DEF_STATE) {
          int absolute = base + word;
          Integer existing = symbolTable.get(type);
          if (existing != null) {
            logger.severe(type + "=" + existing +
                " This variable is multiply defined; first value used.");
          } else {
            symbolTable.put(type, absolute);
            localDefs.put(type, word);
          }
        } else if (state == USE_STATE) {
          // Pass 1 does nothing here
        } else if (state == PROG_STATE) {
        }
        module.add(type);
        module.add(String.valueOf(word));
      }

      if (state == DEF_STATE) {
        state = USE_STATE;
      } else if (state == USE_STATE) {
        state = PROG_STATE;
      } else if (state == PROG_STATE) {
        for (Map.Entry<String, Integer> localDef : localDefs.entrySet()) {
          if (localDef.getValue() >= n) {
            logger.severe("The value of " + localDef.getKey()
                + " is outside of its module; zero (relative) used.");
            symbolTable.put(localDef.getKey(), base);
          }
        }
        localDefs = new HashMap<String, Integer>();
        modules.put(base, module);
        state = DEF_STATE;
        module = new ArrayList<String>();
        base += n;
      }
    }
  }

  /**
   * Pass 2 processes each module one at a time.  It collects the program text
   * of each module, and then performs a series of operations on that program
   * text.  These operations include primarily walking the tree of external 
   * references, but also checks for a number of error conditions.
   *
   * @return List of words that are the Linked program output.
   */
  public List<Integer> pass2() {
    Set<String> found = new HashSet<String>();
    List<Integer> output = new ArrayList<Integer>();
    for (Map.Entry<Integer, List<String>> entry : modules.entrySet()) {
      int base = entry.getKey();
      int state = DEF_STATE;

      // The raw module to process, as a list of strings. Stored in pass1
      List<String> module = entry.getValue();
      // moduleOutput just records a linked word list for this module
      List<Integer> moduleOutput = new ArrayList<Integer>();
      // uses records where each definition is used in this module
      Map<String, Integer> uses = new HashMap<String, Integer>();

      for (Iterator<String> partIterator = module.iterator(); partIterator.hasNext();) {
        int n = 0;
        try {
          n = (int) Double.parseDouble(partIterator.next());
        } catch (NumberFormatException e) {}

        // Get a list of unprocessed words that we will process in the next
        // block
        for (int i = 0; i < n && partIterator.hasNext(); i++) {
          String type = partIterator.next();
          Integer word = null;
          try {
            word = (int) Double.parseDouble(partIterator.next());
          } catch (NumberFormatException e) {
          } catch (NoSuchElementException e) {}
          if (state == DEF_STATE) {
            // Pass 2 does nothing here
          } else if (state == USE_STATE) {
            // Record this use for future processing
            uses.put(type, word);
          } else if (state == PROG_STATE) {
            if (type.equals("I") || type.equals("A")) {
              // Do nothing for Immediate or Absolute addresses 
              moduleOutput.add(word);
            } else if (type.equals("R")) {
              // + modifier so that we can tell later that this is R
              moduleOutput.add(base + word + RELATIVE_MODIFIER); 
            } else if (type.equals("E")) {
              // Resolve this later when we walk through each external ref
              // + modifier so that we can tell later that this is E
              moduleOutput.add(word + EXTERNAL_MODIFIER); 
            }
          }
        }

        if (state == DEF_STATE) {
          state = USE_STATE;
        } else if (state == USE_STATE) {
          state = PROG_STATE;
        } else {
          state = DEF_STATE;

          // For each use, mark it found, and update all references to it
          for (Map.Entry<String, Integer> use : uses.entrySet()) {
            String varName = use.getKey();
            int firstUseAddress = use.getValue();
            found.add(varName);

            // Calculate the absolute address from the symbol table
            Integer absoluteVarAddress = symbolTable.get(varName);
            if (absoluteVarAddress == null) {
              logger.severe(varName + " is not defined; zero used.");
              absoluteVarAddress = 0;
            }

            // currentUse is the last 3 digits, because first digit is opcode
            int currentAddress = firstUseAddress % 1000;
            while (currentAddress != 777) {
              if (currentAddress > moduleOutput.size()) {
                logger.severe("Pointer in use chain exceeds module size; "
                    + "chain terminated.");
                break;
              }
              int currentValue = moduleOutput.get(currentAddress);
              // The order of the following two if statements is REQUIRED
              // because of how states are managed in words
              if (currentValue / RELATIVE_MODIFIER != 0) {
                // Account for the added base in relative addresses
                // We don't need that now, we're treating it as E
                currentValue = currentValue - RELATIVE_MODIFIER - base;
              }
              if (currentValue / EXTERNAL_MODIFIER == 0) {
                // This isn't an E, but we're in a use, so throw error
                logger.severe("Non-E type address on use chain; treated as E type.");
              } else {
                currentValue = currentValue - EXTERNAL_MODIFIER;
              }

              // Calculate the next use, then the new value for this word
              int nextAddress = currentValue % 1000;
              int newWord = (currentValue / 1000) * 1000 + absoluteVarAddress % 1000;
              moduleOutput.set(currentAddress, newWord);
              currentAddress = nextAddress;
            }
          }

          // Clean up any modified words that still remain
          // as we no longer need their state
          for (int i = 0; i < moduleOutput.size(); i++) {
            int value = moduleOutput.get(i);
            if (value / RELATIVE_MODIFIER != 0) {
              moduleOutput.set(i, value - RELATIVE_MODIFIER);
            } else if (value / EXTERNAL_MODIFIER != 0) {
              logger.warning("E type address not on use chain; treated as "
                  + "I type.");
              moduleOutput.set(i, value - EXTERNAL_MODIFIER);
            }
          }
          output.addAll(moduleOutput);
        }
      }
    }

    // Check for unused definitions and notify about any found
    for (String key : symbolTable.keySet()) {
      if (!found.contains(key)) {
        logger.warning(key + " was defined but never used.");
      }
    }

    return output;
  }

  /**
   * Has the given word been translated to indicate it is relative?
   */
  protected boolean isRelative(int value) {
    return (value / RELATIVE_MODIFIER) == 0;
  }

  /**
   * Has the given word been translated to indicate it is external?
   */
  protected boolean isExternal(int value) {
    return (value / EXTERNAL_MODIFIER) == 0;
  }

  /**
   * Calls each pass of the linker, and formats the output to match that of
   * the given samples.
   *
   * @return Symbol table and memory map in String form after linking.
   */
  public String link() {
    String output = "";
    pass1();
    output += "Symbol Table\n";
    for (Map.Entry<String, Integer> symbol : symbolTable.entrySet()) {
      output += symbol.getKey() + "=" + symbol.getValue() + "\n";
    }
    output += "\nMemory Map\n";
    int i = 0;
    List<Integer> words = pass2();
    for (int word : words) {
      // Just some simple whitespace formatting to match samples
      if ((i / 10) >= 1) {
        output += i + ": ";
      } else {
        output += i + ":  ";
      }
      output += word + "\n";
      i++;
    }
    return output;
  }

  /**
   * Given an input file, link it and output the result.
   *
   * @param args Command-line arguments.
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java Linker path-to-input-file");
      System.exit(1);
    }
    try {
      Reader reader = new BufferedReader(new InputStreamReader(
          new FileInputStream(args[0])));
      Linker linker = new Linker(reader);
      System.out.println(linker.link());
    } catch (FileNotFoundException e) {
      System.err.println("Could not find file: " + args[0]);
      System.exit(1);
    }
  }
}
