
class Bean {
    
    Map getTestMap() {
        List foo = ['bar']
        Map map = [ 'foo' : foo]
        return map
    }

    String getNullValue() {
        return null
    }
}

@org.rioproject.config.Component('bean.config')
class BeanExtended extends Bean {

    String getFood() {
        return "yummy"
    }

    String getFood(String s) {
        return getFood()+' '+s
    }

    Map getTestMap() {
        Map map = super.getTestMap()
        List foo = map.get('foo')
        foo.add('baz')
        map.put('foo', foo)
        return map
    }
}

class Config {
    Map getWithClosure() {
        def map = [:]
        ['foo', 'bar', 'baz'].each { el ->
            map.put(el, el.reverse())
        }
        return map
    }

    long getLongValue() {
        return 99
    }
}