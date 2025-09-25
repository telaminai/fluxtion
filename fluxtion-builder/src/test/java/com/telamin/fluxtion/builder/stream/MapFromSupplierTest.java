package com.telamin.fluxtion.builder.stream;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.test.util.MultipleSepTargetInProcessTest;
import com.telamin.fluxtion.builder.test.util.SepTestConfig;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Mappers;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Predicates;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MapFromSupplierTest extends MultipleSepTargetInProcessTest {

    public static class EvenCounter{
        private int count = 0;

        public int getCount(){
            return count;
        }

        public void incrementIfEven(int input){
            if(input % 2 != 0) return;
            count++;
        }
    }

    public MapFromSupplierTest(SepTestConfig testConfig) {
        super(testConfig);
    }

    @Test
    public void hasChangedInt() {
        sep(c -> {
            EvenCounter counter = new EvenCounter();
            DataFlowBuilder.subscribe(Integer.class)
                    .push(counter::incrementIfEven)
                    .mapFromSupplier(counter::getCount)
                    .mapToInt(Integer::intValue)
                    .filter(Predicates.hasIntChanged())
                    .mapOnNotify(Mappers.newCountNode()).id("count");
        });
        Mappers.CountNode countNode = getStreamed("count");

        onEvent((Integer) 20);
        assertThat(countNode.getCount(), is(1));

        onEvent((Integer) 11);
        assertThat(countNode.getCount(), is(1));

        onEvent((Integer) 22);
        assertThat(countNode.getCount(), is(2));

        onEvent((Integer) 20);
        assertThat(countNode.getCount(), is(3));

        onEvent((Integer) 255);
        assertThat(countNode.getCount(), is(3));
    }
}
