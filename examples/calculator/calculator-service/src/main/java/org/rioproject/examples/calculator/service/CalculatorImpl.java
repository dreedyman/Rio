/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.examples.calculator.service;

import org.rioproject.examples.calculator.*;

import java.rmi.RemoteException;

/**
 * An implementation of the Calculator service.
 */
public class CalculatorImpl implements Calculator {
    private Add addService;
    private Subtract subtractService;
    private Multiply multiplyService;
    private Divide divideService;

    public void setAdd(Add addService) {
        this.addService = addService;
    }

    public void setSubtract(Subtract subtractService) {
        this.subtractService = subtractService;
    }

    public void setDivide(Divide divideService) {
        this.divideService = divideService;
    }

    public void setMultiply(Multiply multiplyService) {
        this.multiplyService = multiplyService;
    }

    public double add(double n1, double n2) throws RemoteException {
        return(addService.add(n1, n2));
    }

    public double subtract(double n1, double n2) throws RemoteException {
        return(subtractService.subtract(n1, n2));
    }

    public double multiply(double n1, double n2) throws RemoteException {
        return(multiplyService.multiply(n1, n2));
    }

    public double divide(double n1, double n2) throws RemoteException {
        return(divideService.divide(n1, n2));
    }

}
