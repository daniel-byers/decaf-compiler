#!/bin/bash

# Daniel Byers | 13121312

# If this script is called without any arguments, it will clean the directory and rebuild compiler.
# Command Line Arguments: 
#  - First is type of tests to run; will loop through all related files and run each individually.
#  - Second is debug flag; only errors will be output from compiler if this is not present.


rm testdata/**/*.scan
rm testdata/**/*.parse
rm testdata/**/*.ir
rm testdata/**/*.s

ant clobber
ant clean
ant

case $1 in
    'scan')
            for f in testdata/scanner/*
            do
              if [ $f != "testdata/scanner/output" ]
              then
                echo "---------------------------------------------------------"
                java -cp lib/antlr-4.6-complete.jar:Compiler.jar decaf.Main $2 -target scan $f
              fi
            done

            for f in testdata/scanner/*.scan
            do
              filename=$(basename "$f")
              filename="${filename%.*}"
              echo "---------------------------------------------------------"
              diff -s -y -a $f "testdata/scanner/output/${filename}.out" #-q #(quiet)
            done
       ;;

    'parse')
            for f in testdata/parser/*
            do
              echo "---------------------------------------------------------"
              cat $f
              java -cp lib/antlr-4.6-complete.jar:Compiler.jar decaf.Main $2 -target parse $f
            done        
       ;;

    'inter')
            for f in testdata/semantics/illegal*
            do
              echo "---------------------------------------------------------"
              cat $f | grep -o '//.*'
              java -cp lib/antlr-4.6-complete.jar:Compiler.jar decaf.Main $2 -target inter $f
            done

            echo "---------------------------------------------------------"
            echo "legal file follows. Should be no errors output"
            java -cp lib/antlr-4.6-complete.jar:Compiler.jar decaf.Main -target inter testdata/semantics/legal-01.dcf
            echo "---------------------------------------------------------"
       ;;

    'codegen')
            for f in testdata/codegen/*
            do
              if [ $f != "testdata/codegen/output" ]
                then
                echo "---------------------------------------------------------"
                echo $f
                java -cp lib/antlr-4.6-complete.jar:Compiler.jar decaf.Main $2 -target codegen $f
              fi
            done
       ;;
esac