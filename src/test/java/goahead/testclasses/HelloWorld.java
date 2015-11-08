package goahead.testclasses;

import goahead.ExpectedOutput;

@ExpectedOutput("Hello world\n")
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello world");
    }
}

