#!/bin/sh
java -Dprism.lcdtext=false --module-path /usr/share/openjfx/lib --add-modules=javafx.swing,javafx.web MarkdownReader $1
