/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
({
    assertChangeEvent: function(component, value){
        $A.test.assertTrue(undefined !== component._log, "change handler not invoked");
        $A.test.assertEquals(1, component._log.length, "unexpected number of change events recorded");
        if (value instanceof Array) {
        	var actual = component._log[0].value;
        	$A.test.assertEquals(value.length, actual.length, "Unexpected value of change length mismatch");
        	for (var i = 0; i < value.length; i++) {
        		// Deal with nested-array support if and when we need it.
        		$A.test.assertEquals(value[i], actual[i], "Unexpected value of change at index " + i);
        	}
        } else {
        	// We punt on map support until we need it.
            $A.test.assertEquals(value, component._log[0].value, "unexpected value of change");
        }
        component._log = undefined; // reset log
    },

    assertNoChangeEvent: function(component){
        $A.test.assertEquals(undefined, component._log);
    },

    calculateSize: function(map) {
        return $A.util.keys(map).length;
    },
    /**
     * Verify creating map values.
     */
    //TODO ##$$: RJ Refactor this test case after halo work, the "halo" branch has the correct code
    //##$$ Remove this line
    //##$$ uncomment this line
    testCreateMapValue:{
		test: [function(component){
			    var mval = $A.expressionService.create(null, {"string":"something","integer":23,"boolean":true});
			    mval = mval.unwrap(); //##$$ Remove this line
	            $A.test.assertTrue($A.util.isObject(mval), "expected a map");
	            $A.test.assertEquals(3, this.calculateSize(mval), "expected 3 values");
	            $A.test.assertEquals("something", mval["string"], "expected string value");
	            $A.test.assertEquals(23, mval["integer"], "expected integer value");
	            $A.test.assertEquals(true, mval["boolean"], "expected boolean value");
	        },function(component){
	        	var mval = $A.expressionService.create(null, {});
	        	mval = mval.unwrap(); //##$$ Remove this line
	            $A.test.assertTrue($A.util.isObject(mval), "expected a empty map");
	            $A.test.assertEquals(0, this.calculateSize(mval), "expected 0 values");
	        }]
    },
    /**
     * Test getting a property reference from a MapValue.
     */
    //##$$ uncomment this line after halo changes are in master. Previouls used to fail because of W-1563175
    _testGetWithPropertyReference:{
        test:function(component){
        	$A.log(component.find('htmlDiv'));
        	var newMap = component.find('htmlDiv').get('v.HTMLAttributes');
            try{
            	$A.test.assertTrue($A.util.isExpression(newMap["disabled"]));
                $A.test.assertEquals(false, newMap["disabled"].evaluate(), "failed to resolve propertyReferenceValue");
            }catch(e){
                $A.test.fail("Failed to resolve PropertyReferenceValues. Error :" + e);
            }
        }
    },

    /**
     * Setting map value to Map attribute type should use the provided value.
     */
    //W-2310538
    _testSetMapValue: {
    	attributes:{
    		map : "{'do':'something','it':'the','right':'way'}"
    	},
        test: [
            /*W-2251243, can we be intelligent about this
            function(component){ //Set value but new value is same as old value
           	   var mval = component.get("v.map");
           	   component.set("v.map", mval);
               this.assertNoChangeEvent(component);
           },*/
           function(component){ //Add new values to map and set attribute
	        	var mval = component.get("v.map");
	            mval["newKey"] = "newValue";
	            component.set("v.map",mval);
	            this.assertChangeEvent(component, mval);
	            mval = component.get("v.map");
	            $A.test.assertTrue($A.util.isObject(mval), "expected an Map");
	            
	            $A.test.assertEquals(4, this.calculateSize(mval), "expected 4 values");
	            $A.test.assertEquals("something", mval["do"], "wrong first value");
	            $A.test.assertEquals("the", mval["it"], "wrong second value");
	            $A.test.assertEquals("way", mval["right"], "wrong third value");
	            $A.test.assertEquals("newValue", mval["newKey"], "Map attribute not updated");
           }, function(component){ // Update map value - update value of a key
	       	    var mval =  component.get("v.map");
	       	    mval["newKey"] = "updatedValue"
	            component.set("v.map",mval);
	            this.assertChangeEvent(component, mval);
	            mval = component.get("v.map");
	            $A.test.assertEquals(4, this.calculateSize(mval), "expected 4 values");
	            $A.test.assertEquals("updatedValue", mval["newKey"], "wrong map value");
           }, function(component){ //Update map value - remove key
        	    var mval =  component.get("v.map");
	       	    delete mval["newKey"];
	       	    component.set("v.map",mval);
	            this.assertChangeEvent(component, mval);
	            mval = component.get("v.map");
	            $A.test.assertEquals(3, this.calculateSize(mval), "expected 4 values");
	            $A.test.assertUndefined(mval["newKey"]);
           }, function(component){ // Set to new map value
        	    var mval =  $A.expressionService.create(null, {"string":"something"});
        	    mval = mval.unwrap(); //##$$ Remove this line
	            component.set("v.map",mval);
	            this.assertChangeEvent(component, mval);
	            mval = component.get("v.map");
	            $A.test.assertTrue($A.util.isObject(mval), "expected an Map");
	            $A.test.assertEquals(1, this.calculateSize(mval), "expected 1 values");
	            $A.test.assertEquals("something", mval["string"], "wrong map value");
           }]
    },
    
    /**
     * Setting a Map type attribute to a simple value that is not null should fail.
     */
    //W-2251248
    _testSetSimpleValue: {
        test: function(component){
            var sval = $A.expressionService.create(null, "simple string");
            var errMsg = "Expected exception from set(simpleValue)";
            try {
            	component.set("v.map", sval);
                $A.test.fail(errMsg);
            } catch (e) {
            	if(e.message == errMsg){
            		$A.test.fail(errMsg);
            	}
            }
        }
    },

    /**
     * Setting a Map type attribute to a simple value that is null should clear the map.
     */
    testSetSimpleValueNull: {
        test: function(component){
        	var sval = $A.expressionService.create(null, null);
            sval = sval.unwrap(); //##$$ Remove this line
            $A.test.assertNull(sval);
            component.set("v.map", sval);
            var map = component.get("v.map")
            $A.test.assertTrue($A.util.isObject(map)); //##$$ Remove this line
    		$A.test.assertEquals(0, $A.util.keys(map).length); //##$$ Remove this line
            //$A.test.assertNull(component.get("v.map")); ##$$ uncomment this line
        }
    },
    /**
     * Setting a Map type attribute to a simple value that is undefined should clear the map.
     */
    testSetSimpleValueUndefined: {
        test: function(component){
        	var sval = $A.expressionService.create(null);
        	sval = sval.unwrap(); //##$$ Remove this line
        	$A.test.assertUndefined(sval);
            component.set("v.map", sval);
            var map = component.get("v.map")
            $A.test.assertTrue($A.util.isObject(map)); //##$$ Remove this line
    		$A.test.assertEquals(0, $A.util.keys(map).length); //##$$ Remove this line
            //$A.test.assertUndefined(component.get("v.map")) ##$$ uncomment this line
        }
    },

    //Fails in Halo due to W-2256415, Setting new Maps as model values doesn't work. At least not similar to attributes
    testMapSetValueRenders: {
        test: [ function(component) {
            var map = component.get("m.map");
            map["subkey"] = "put";
            component.set("m.map", map);
            // Insert a pause for re-rendering.  Put of a "new" key is CLEAN,
            // perhaps oddly, so it doesn't re-render:
            $A.test.addWaitFor("", function() {
            	var output = component.find("outputText");
                return $A.test.getText(output.getElement());
            });
        }, function(component) {
           var map = component.get("m.map");
            map["subkey"] = "put2";
            component.set("m.map", map);
            // Insert a pause for re-rendering.  Put of a "old" key is DIRTY,
            // in the usual "I've been changed" way, so it does re-render:
            $A.test.addWaitFor("put2", function() {
                var output = component.find("outputText");
                return $A.test.getText(output.getElement());
            });
        }, function(component) {
            var map = {"subKey": "set"};
            component.set("m.map", map);
            // Insert a pause for re-rendering.  SetValue leaves DIRTY child
            // objecst (W-1678810), so it does re-render.  Note that this also
            // tests our case-insensitivity.
            $A.test.addWaitFor("set", function() {
                var output = component.find("outputText");
                return $A.test.getText(output.getElement());
            });
        }, function(component) {
            // Checks case insensitivity
            var otherMap = $A.expressionService.create(null, { 'subkey' : "second" });
            component.set("m.map", otherMap);
            $A.test.addWaitFor("second", function() {
                var output = component.find("outputText");
                return $A.test.getText(output.getElement());
            });
        }
        ]
    }
})
