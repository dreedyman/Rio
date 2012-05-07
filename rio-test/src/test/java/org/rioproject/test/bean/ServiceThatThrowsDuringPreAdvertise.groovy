package org.rioproject.test.bean

import org.rioproject.bean.PreAdvertise

/**
 * 
 * @author Dennis Reedy
 */
class ServiceThatThrowsDuringPreAdvertise {

    @PreAdvertise
    def preAdv() {
        throw new RuntimeException("foo")
    }
}
