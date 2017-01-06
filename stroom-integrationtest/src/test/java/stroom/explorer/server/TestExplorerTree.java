package stroom.explorer.server;

import org.junit.Test;
import stroom.AbstractCoreIntegrationTest;

import javax.annotation.Resource;
import java.util.UUID;

public class TestExplorerTree extends AbstractCoreIntegrationTest {
    @Resource
    private ExplorerTreeService explorerTreeService;
    
    @Test
    public void testCreateTree() throws Exception {
        ExplorerTreeNode root = explorerTreeService.createRoot(newTreePojo("System"));
        ExplorerTreeNode a = explorerTreeService.addChild(root, newTreePojo("A"));
        ExplorerTreeNode b = explorerTreeService.addChild(root, newTreePojo("B"));
        ExplorerTreeNode c = explorerTreeService.addChild(root, newTreePojo("C"));
        explorerTreeService.addChild(b, newTreePojo( "B1"));
        explorerTreeService.addChild(b, newTreePojo("B2"));
        explorerTreeService.addChild(a, newTreePojo( "A1"));
        ExplorerTreeNode c1 = explorerTreeService.addChild(c, newTreePojo( "C1"));
        explorerTreeService.addChild(c1, newTreePojo( "C11"));
//        outputTree(root, dao);

        //commitDbTransaction(session, "insert tree nodes");
//        return root.getId();
    }
//
//    protected ClosureTableTreeDao newDao(final DbSession session)	{
//        ClosureTableTreeDao dao =
//                new ClosureTableTreeDao(
//                        ExplorerTreeNode.class,
//                        ExplorerTreePath.class,
//                        false,
//                        session);
//
//        dao.setRemoveReferencedNodes(true);
//
////        if (isTestCopy() == false)
////            dao.setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());
//
//        return dao;
//    }

    private ExplorerTreeNode newTreePojo(final String name) {
        final ExplorerTreeNode explorerTreeNode = new ExplorerTreeNode();
        explorerTreeNode.setName(name);
        explorerTreeNode.setType("test");
        explorerTreeNode.setUuid(UUID.randomUUID().toString());
        return explorerTreeNode;
    }
}
