#!/usr/bin/env groovy
/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


def docsDir = new File("../docs");
if (!docsDir.exists())
    docsDir = new File(System.getProperty("user.dir"), "docs")

def getReplacement(String depth) {
    return "        <a href=\"https://github.com/dreedyman/Rio\"><img style=\"position: absolute; top: 0; right: 0; border: 0; z-index: 1000000;\" src=\"" + depth + "images/forkme_right_green_007200.png\" alt=\"Fork me on GitHub\"></a>\n" +
           "<div class=\"navbar navbar-fixed-top\">"
}

def dirs = [new File(docsDir, "examples"), new File(docsDir, "opstring"), new File(docsDir, "tutorial")]

def fixItUp(file) {
    String depth = "./"
    println "parent: ${file.getParentFile().name}"
    if (file.getParentFile().name.equals("opstring")) {
        depth = "../"
    } else if (file.getParentFile().getParentFile().name.equals("examples") ||
               file.getParentFile().getParentFile().name.equals("tutorial")) {
        depth = "../../"
    }
    fileText = file.text;
    backupFile = new File(file.path + ".bak");
    backupFile.write(fileText);

    /* This will orient the top toc to the left */
    fileText = fileText.replaceAll("<ul class=\"nav pull-right\">", "<ul class=\"nav pull-left\">")

    /* The places the "Fork me on GitHub" image in the upper right */
    fileText = fileText.replaceAll("<div class=\"navbar navbar-fixed-top\">", getReplacement(depth))

    file.write(fileText);
    backupFile.delete()
}

docsDir.eachFile { file ->
    if (file.name.endsWith(".html"))
        fixItUp(file)
}

dirs.each { dir ->
    dir.eachFileRecurse({ file ->
        if (file.name.endsWith(".html"))
            fixItUp(file)
    })
}
