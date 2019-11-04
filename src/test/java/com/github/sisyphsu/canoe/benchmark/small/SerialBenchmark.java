package com.github.sisyphsu.canoe.benchmark.small;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sisyphsu.canoe.Canoe;
import com.github.sisyphsu.canoe.CanoePacket;
import com.github.sisyphsu.canoe.CanoeStream;
import com.github.sisyphsu.canoe.node.Node;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                 Mode  Cnt     Score    Error  Units
 * SerialBenchmark.json      avgt    6   722.865 ± 59.575  ns/op
 * SerialBenchmark.packet    avgt    6  1275.657 ± 43.733  ns/op
 * SerialBenchmark.protobuf  avgt    6   198.970 ±  2.297  ns/op
 * SerialBenchmark.stream    avgt    6   621.402 ± 19.251  ns/op
 * <p>
 * Need more works to do to improve performace~
 * <p>
 * 190ns for Output#scan
 * 500ns for Output#doWrite and others
 * <p>
 * Benchmark                 Mode  Cnt     Score    Error  Units
 * SerialBenchmark.json      avgt    6   865.418 ± 42.638  ns/op
 * SerialBenchmark.packet    avgt    6  1220.496 ± 42.971  ns/op
 * SerialBenchmark.protobuf  avgt    6   243.895 ± 10.057  ns/op
 * SerialBenchmark.stream    avgt    6   603.656 ± 11.698  ns/op
 *
 * @author sulin
 * @since 2019-10-28 17:32:33
 */
@Warmup(iterations = 2, time = 2)
@Fork(2)
@Measurement(iterations = 3, time = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SerialBenchmark {

    static final Date date = new Date();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final UserModel    USER          = UserModel.random();
    private static final CanoeStream  STREAM        = new CanoeStream();

    @Benchmark
    public void json() throws JsonProcessingException {
        OBJECT_MAPPER.writeValueAsString(USER.toModel());
    }

    @Benchmark
    public void packet() throws IOException {
        CanoePacket.serialize(USER.toModel());
    }

    @Benchmark
    public void stream() throws IOException {
        STREAM.serialize(USER.toModel());
    }

    @Benchmark
    public void protobuf() {
        USER.toPB().toByteArray();
    }

    //    @Benchmark
    public void toNode() {
//        USER.toModel(); // 27ns

//        Canoe.CODEC.getPipeline(UserModel.class, Node.class); // 11ns -> 5ns

        // 441ns, CodecContext cost 20ns
        // 123ns, 48ns if not convert value, 75ns if no Date
        // [date -> node] cost 100ns???
//        USER.setCreateTime(null);
//        beanNodeCodec.toNode(USER);
//        CodecContext.reset();

        // 123ns
//        beanNodeCodec.toNode(USER);
//        CodecContext.reset();

//        Canoe.CODEC.convert(date, Node.class); // 60ns -> 17ns

        // 380ns, Convert all fields into Node
//        BeanHelper helper = BeanHelper.valueOf(USER.getClass());
//        String[] names = helper.getNames();
//        Object[] values = helper.getValues(USER);
//        for (int i = 0, len = values.length; i < len; i++) {
//            values[i] = values[i];
//        }
//        new ObjectNode(true, names, values);

        // 263ns = 27ns(toModel) + 11ns(getPipeline) + 178ns(BeanNodeCodec.toNode)
        // Pipeline.convert cost 50ns ???
        // use ASM optimize ConverterPipeline, 263ns -> 155ns
        Canoe.CODEC.convert(USER.toModel(), Node.class);

        // 169ns = toModel[27ns] + beanNodeCodec#toNode[123ns] + getPipeline[5ns] + [14ns]
        // CodecContext/ThreadLocal may cost 20~30ns
    }

}
