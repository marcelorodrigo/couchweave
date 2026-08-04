package io.github.marcelorodrigo.couchweave.build.fixtures.coveragefailure;

final class UndercoveredFixture {

    int coveredValue() {
        return 1;
    }

    int uncoveredValue() {
        return 2;
    }
}
