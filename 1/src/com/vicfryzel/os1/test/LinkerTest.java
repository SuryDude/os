// Copyright 2010 Vic Fryzel.  All rights reserved.

package com.vicfryzel.os1.test;

import com.vicfryzel.os1.Linker;

import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import junit.framework.TestCase;


public class LinkerTest extends TestCase {
  private Linker getLinkerForFile(String path) throws FileNotFoundException {
    Reader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(path)));
    return new Linker(reader);
  }

  private String readOutputFile(String path) throws Exception {
    String data = "";
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(path)));
    String line;
    while ((line = reader.readLine()) != null) {
      data += line + "\n";
    }
    return data;
  }

  public void testInputMatchesOutput() throws Exception {
    String dataPath = System.getProperty("data.dir");
    if (dataPath == null) {
      dataPath = "";
    } else {
      dataPath += "/";
    }
    int i = 1;
    String inputFilePath, outputFilePath;
    while (i <= 9) {
      inputFilePath = dataPath + "input-" + i;
      outputFilePath = dataPath + "output-" + i;
      Linker linker = getLinkerForFile(inputFilePath);
      String output = linker.link();
      assertEquals(output, readOutputFile(outputFilePath));

      i++;
    }
  }
}
