package com.intel.dai.inventory.utilities;

import org.spockframework.lang.ISpecificationContext;
public class Helper {
    static public String testStartMessage(ISpecificationContext specificationContext) {
        return ConsoleColor.cyan(
                String.format(
                ">>>>>> %s::%s",
                        specificationContext.getCurrentSpec().getName(),
                        specificationContext.getCurrentIteration().getName()));
    }
    static public String testEndMessage(ISpecificationContext specificationContext) {
        return ConsoleColor.cyan(
                String.format(
                        "<<<<<< %s::%s",
                        specificationContext.getCurrentSpec().getName(),
                        specificationContext.getCurrentIteration().getName()));
    }
}
