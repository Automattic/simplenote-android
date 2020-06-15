package com.automattic.simplenote.smoke.support;

import java.util.function.Supplier;

public class SupplierIdler extends SimplenoteIdler {
    private Supplier<Boolean> mSupplier;

    public SupplierIdler(Supplier<Boolean> supplier) {
        mSupplier = supplier;
    }

    @Override
    public boolean checkCondition() {
        return mSupplier.get();
    }
}