package excelAssertions;

import org.apache.poi.ss.usermodel.Cell;
import org.assertj.core.api.SoftAssertions;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = false)
public final class SimpleCellAssertion extends CellAssertion<SimpleCellAssertion> {
    public SimpleCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    @Override
    protected void applyAssertCore(Cell cell, SoftAssertions softly) {
        //do nothing. This will only be used to assert on format, format category etc
    }
}
