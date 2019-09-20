package dev.onload.zookeeper.curator;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-20 02:02
 * @description
 */
public class TestBuilder {
    private int name;
    private int age;

    private TestBuilder(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    public static Builder newTestBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private int name;
        private int age;

        private Builder() {
        }

        public TestBuilder build() {
            return new TestBuilder(this);
        }

        public Builder name(int name) {
            this.name = name;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }
    }

    public static void main(String[] args) {
        System.out.println(TestBuilder.newTestBuilder().age(2).name(1).build());
    }
}
