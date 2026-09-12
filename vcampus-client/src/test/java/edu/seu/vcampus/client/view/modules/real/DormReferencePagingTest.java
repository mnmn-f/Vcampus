package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DormReferencePagingTest {
    @Test public void referenceChoicesReadAllPagesWithinServerLimit() throws Exception {
        List<Integer> source = new ArrayList<>(); for (int i = 0; i < 251; i++) source.add(i);
        List<Integer> result = DormSpaceEditorPanel.allReferences(query -> {
            assertTrue(query.getPageSize() <= 100);
            int start = (query.getPage() - 1) * query.getPageSize();
            return new DormPage<>(query.getPage(), query.getPageSize(), source.size(), source.subList(start, Math.min(start + query.getPageSize(), source.size())));
        });
        assertEquals(source, result);
    }
}
