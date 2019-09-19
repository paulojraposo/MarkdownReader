#!/bin/sh
java --module-path /usr/share/openjfx/lib --add-modules=javafx.swing,javafx.web MarkdownReader $1
