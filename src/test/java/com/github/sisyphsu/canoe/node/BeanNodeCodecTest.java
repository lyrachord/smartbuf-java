package com.github.sisyphsu.canoe.node;

import com.github.sisyphsu.canoe.convertor.CodecFactory;
import com.github.sisyphsu.canoe.node.basic.ObjectNode;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * @author sulin
 * @since 2019-10-21 11:12:53
 */
public class BeanNodeCodecTest {

    private NodeCodec codec = new NodeCodec();

    @BeforeEach
    void setUp() {
        CodecFactory.Instance.installCodec(NodeCodec.class);

        codec.setFactory(CodecFactory.Instance);
    }

    @Test
    public void testMap() {
        Map<Object, Object> map = new HashMap<>();

        assert codec.toNode(map) == ObjectNode.EMPTY;
        assert codec.toMap(ObjectNode.EMPTY).isEmpty();

        map.put("id", RandomUtils.nextLong());
        map.put("name", RandomStringUtils.randomAlphanumeric(16));
        map.put("score", RandomUtils.nextDouble());
        map.put(System.currentTimeMillis(), RandomUtils.nextDouble());
        Node node = codec.toNode(map);
        assert node instanceof ObjectNode;

        Map<String, Object> map2 = codec.toMap((ObjectNode) node);
        assert map.size() == map2.size();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object val = entry.getValue();
            Object val2 = map2.get(String.valueOf(key));
            assert Objects.equals(val, val2);
        }
    }

    @Test
    public void testBean() {
        Person person = new Person();
        Node node = codec.toNode(person);

        assert node instanceof ObjectNode;

        Person person1 = CodecFactory.Instance.convert(node, Person.class);

        assert person.equals(person1);
    }

    @Test
    public void testArray() {
        Group group = new Group();
        group.persons.add(new Person());
        group.persons.add(new Person());
        group.persons.add(new Person());
        group.persons.add(new Person());
        group.persons.add(new Person());

        Node node = CodecFactory.Instance.convert(group, Node.class);
        assert node instanceof ObjectNode;

        Group group1 = CodecFactory.Instance.convert(node, Group.class);
        assert group.equals(group1);
    }

    @Data
    public static class Person {
        private int     id    = RandomUtils.nextInt();
        private float   score = RandomUtils.nextFloat();
        private boolean old   = RandomUtils.nextBoolean();
        private String  name  = RandomStringUtils.randomAlphanumeric(16);
        private Object  obj;
    }

    @Data
    public static class Group {
        private Date         date    = new Date();
        private Thread.State state   = Thread.State.BLOCKED;
        private List<Person> persons = new ArrayList<>();
    }

}
