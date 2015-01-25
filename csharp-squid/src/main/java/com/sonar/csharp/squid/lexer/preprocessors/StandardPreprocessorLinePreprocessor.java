/*
 * Sonar C# Plugin :: C# Squid :: Squid
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.csharp.squid.lexer.preprocessors;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.csharp.squid.CSharpConfiguration;
import com.google.common.collect.Lists;
import com.sonar.csharp.squid.api.CSharpTokenType;
import com.sonar.sslr.api.Preprocessor;
import com.sonar.sslr.api.PreprocessorAction;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

public class StandardPreprocessorLinePreprocessor extends Preprocessor {

  private ArrayList<String> definedSymbols = new ArrayList<String>();
  private ArrayList<String> locallyDefinedSymbols = new ArrayList<String>();

  public StandardPreprocessorLinePreprocessor(CSharpConfiguration conf) {
    definedSymbols.addAll(conf.getDefinedSymbols());
  }

  @Override
  public PreprocessorAction process(List<Token> tokens) {
    Token token = tokens.get(0);

    if (token.getType() == GenericTokenType.EOL) {
      locallyDefinedSymbols.clear();
      return PreprocessorAction.NO_OPERATION;
    }
    if (token.getType() != CSharpTokenType.PREPROCESSOR) {
      return PreprocessorAction.NO_OPERATION;
    }

    return ProcessConditions(tokens);
  }

  private PreprocessorAction ProcessConditions(List<Token> tokens) {
    ArrayList<Trivia> triviasToInsert = new ArrayList<Trivia>();
    ArrayList<Token> tokensToInsert = new ArrayList<Token>();
    int tokenPos = ProcessConditions(tokens, 0, new ArrayList<Boolean>(), triviasToInsert, tokensToInsert);
    return new PreprocessorAction(tokenPos, triviasToInsert, tokensToInsert);
  }

  private int ProcessConditions(List<Token> tokens, int tokenPos, ArrayList<Boolean> isActiveStack, ArrayList<Trivia> triviasToInsert, ArrayList<Token> tokensToInsert) {
    int position = tokenPos;
    while (position < tokens.size()) {
      Token token = tokens.get(position);
      position++;
      if (token.getType() != CSharpTokenType.PREPROCESSOR) {
        if (IsActive(isActiveStack)) {
          tokensToInsert.add(token);
        } else {
          triviasToInsert.add(Trivia.createSkippedText(token));
        }
      }

      String[] dirAndValue = getDirectiveAndValue(token);
      String dir = dirAndValue[0];
      if (dir.equals("define")) {
        locallyDefinedSymbols.add(dirAndValue[1]);
      } else if (dir.equals("undef")) {
        while (locallyDefinedSymbols.remove(dirAndValue[1])) {
        }
      } else if (dir.equals("if")) {
        isActiveStack.add(isValueDefined(dirAndValue[1]));
      } else if (dir.equals("elif")) {
        isActiveStack.remove(isActiveStack.size() - 1);
        isActiveStack.add(isValueDefined(dirAndValue[1]));
      } else if (dir.equals("else")) {
        isActiveStack.set(isActiveStack.size() - 1, !(isActiveStack.get(isActiveStack.size() - 1)));
      } else if (dir.equals("endif")) {
        isActiveStack.remove(isActiveStack.size() - 1);
        if (isActiveStack.size() == 0) {
          break;
        }
      } else {
        triviasToInsert.add(Trivia.createSkippedText(token));
      }
    }
    return position;
  }

  private Boolean isValueDefined(String str) {
    if (str.equalsIgnoreCase("false")) {
      return false;
    }
    if (str.equalsIgnoreCase("true")) {
      return false;
    }
    for (int i = 0; i < locallyDefinedSymbols.size(); i++) {
      if (str.equalsIgnoreCase(locallyDefinedSymbols.get(i))) {
        return true;
      }
    }
    for (int i = 0; i < definedSymbols.size(); i++) {
      if (str.equalsIgnoreCase(definedSymbols.get(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean IsActive(ArrayList<Boolean> active) {
    for (int i = 0; i < active.size(); i++)
    {
      if (!active.get(i)) {
        return false;
      }
    }
    return true;
  }

  private String[] getDirectiveAndValue(Token token) {
    String tokenValue = token.getValue().substring(1).trim();
    int indexOfSpace = tokenValue.indexOf(' ');
    if (indexOfSpace == -1) {
      return new String[] {tokenValue.toLowerCase()};
    }
    String directive = tokenValue.substring(0, indexOfSpace);
    String value = tokenValue.substring(indexOfSpace + 1);
    return new String[] {directive, value};
  }

}
