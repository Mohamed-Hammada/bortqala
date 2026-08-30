package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.ProjectCostCodeRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.project.infrastructure.WbsNodeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WbsServiceTests {

    private WbsNodeRepository wbsNodeRepository;
    private ProjectRepository projectRepository;
    private ProjectCostCodeRepository projectCostCodeRepository;
    private AuditService auditService;
    private ObjectMapper objectMapper;
    private WbsService wbsService;

    private Project activeProject;

    @BeforeEach
    void setUp() {
        wbsNodeRepository = mock(WbsNodeRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectCostCodeRepository = mock(ProjectCostCodeRepository.class);
        auditService = mock(AuditService.class);
        objectMapper = new ObjectMapper();
        wbsService = new WbsService(wbsNodeRepository, projectRepository, projectCostCodeRepository, auditService, objectMapper);

        activeProject = new Project(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, new BigDecimal("10000000"), "EGP", null, null, true
        );
        activeProject.activate();
        when(projectRepository.findById(activeProject.getId())).thenReturn(Optional.of(activeProject));
    }

    @Test
    void createRootWbsNode_succeeds_withLevelOneAndPath() {
        when(wbsNodeRepository.existsByProjectIdAndWbsCode(activeProject.getId(), "1")).thenReturn(false);
        when(wbsNodeRepository.save(any(WbsNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateWbsNodeRequest request = new CreateWbsNodeRequest(
                null, "1", "أعمال الأساسات والهيكل", "Substructure & Structure",
                "مرحلة تنفيذ الأساسات الخرسانية", WbsNodeType.PHASE, 1,
                null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, WbsNodeStatus.PLANNED
        );

        WbsNodeResponse response = wbsService.createWbsNode(activeProject.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.wbsCode()).isEqualTo("1");
        assertThat(response.wbsPath()).isEqualTo("/1");
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.parentId()).isNull();
        assertThat(response.nodeType()).isEqualTo(WbsNodeType.PHASE);
    }

    @Test
    void createChildWbsNode_succeeds_withInheritedLevelAndPath() {
        WbsNode parent = new WbsNode(
                activeProject.getId(), null, "1", "/1", "أعمال الأساسات", null,
                null, WbsNodeType.PHASE, 1, 0, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );
        when(wbsNodeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(wbsNodeRepository.existsByProjectIdAndWbsCode(activeProject.getId(), "1.1")).thenReturn(false);
        when(wbsNodeRepository.save(any(WbsNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateWbsNodeRequest request = new CreateWbsNodeRequest(
                parent.getId(), "1.1", "الحفر والردم", "Excavation & Backfill",
                null, WbsNodeType.BOQ_ITEM, 1, "م3", new BigDecimal("5000"),
                new BigDecimal("120.00"), null, null, null, WbsNodeStatus.PLANNED
        );

        WbsNodeResponse response = wbsService.createWbsNode(activeProject.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.wbsCode()).isEqualTo("1.1");
        assertThat(response.wbsPath()).isEqualTo("/1/1.1");
        assertThat(response.level()).isEqualTo(2);
        assertThat(response.parentId()).isEqualTo(parent.getId());
        assertThat(response.plannedQuantity()).isEqualTo(new BigDecimal("5000"));
        assertThat(response.unitRate()).isEqualTo(new BigDecimal("120.00"));
        assertThat(response.plannedAmount()).isEqualTo(new BigDecimal("600000.00"));
    }

    @Test
    void createChildWbsNode_blocks_whenDepthExceedsLimit() {
        WbsNode level10Parent = new WbsNode(
                activeProject.getId(), "p9", "1.10", "/1/2/3/4/5/6/7/8/9/10", "مستوى 10", null,
                null, WbsNodeType.WORK_PACKAGE, 10, 0, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );
        when(wbsNodeRepository.findById(level10Parent.getId())).thenReturn(Optional.of(level10Parent));
        when(wbsNodeRepository.existsByProjectIdAndWbsCode(activeProject.getId(), "1.11")).thenReturn(false);

        CreateWbsNodeRequest request = new CreateWbsNodeRequest(
                level10Parent.getId(), "1.11", "تجاوز الحد", "Exceeds Limit",
                null, WbsNodeType.BOQ_ITEM, 1, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );

        assertThatThrownBy(() -> wbsService.createWbsNode(activeProject.getId(), request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("WBS hierarchy cannot exceed 10 levels.");
    }

    @Test
    void getWbsTree_returnsNestedHierarchy() {
        WbsNode root = new WbsNode(
                activeProject.getId(), null, "1", "/1", "المرحلة الأولى", null,
                null, WbsNodeType.PHASE, 1, 0, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );
        WbsNode child = new WbsNode(
                activeProject.getId(), root.getId(), "1.1", "/1/1.1", "حفر الموقع", null,
                null, WbsNodeType.BOQ_ITEM, 2, 0, "م3", new BigDecimal("100"), BigDecimal.TEN,
                null, null, null, WbsNodeStatus.PLANNED
        );

        when(wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(activeProject.getId()))
                .thenReturn(List.of(root, child));

        List<WbsNodeResponse> tree = wbsService.getWbsTree(activeProject.getId());

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).wbsCode()).isEqualTo("1");
        assertThat(tree.get(0).children()).hasSize(1);
        assertThat(tree.get(0).children().get(0).wbsCode()).isEqualTo("1.1");
    }

    @Test
    void repositionWbsNode_preventsCycles() {
        WbsNode root = new WbsNode(
                activeProject.getId(), null, "1", "/1", "المرحلة الأولى", null,
                null, WbsNodeType.PHASE, 1, 0, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );
        WbsNode child = new WbsNode(
                activeProject.getId(), root.getId(), "1.1", "/1/1.1", "حزمة العمل الأولى", null,
                null, WbsNodeType.WORK_PACKAGE, 2, 0, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );

        when(wbsNodeRepository.findById(root.getId())).thenReturn(Optional.of(root));
        when(wbsNodeRepository.findById(child.getId())).thenReturn(Optional.of(child));
        when(wbsNodeRepository.findByProjectId(activeProject.getId())).thenReturn(List.of(root, child));

        // 1. Cannot be own parent
        RepositionWbsNodeRequest selfReq = new RepositionWbsNodeRequest(root.getId(), 0);
        assertThatThrownBy(() -> wbsService.repositionWbsNode(root.getId(), selfReq))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("A node cannot be its own parent");

        // 2. Cannot move root under its child (cycle!)
        RepositionWbsNodeRequest cycleReq = new RepositionWbsNodeRequest(child.getId(), 0);
        assertThatThrownBy(() -> wbsService.repositionWbsNode(root.getId(), cycleReq))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be moved under one of its own descendants");
    }

    @Test
    void deleteWbsNode_blocksWhenNodeHasChildren() {
        WbsNode root = new WbsNode(
                activeProject.getId(), null, "1", "/1", "المرحلة الأولى", null,
                null, WbsNodeType.PHASE, 1, 0, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, WbsNodeStatus.PLANNED
        );
        when(wbsNodeRepository.findById(root.getId())).thenReturn(Optional.of(root));
        when(wbsNodeRepository.existsByProjectIdAndParentId(activeProject.getId(), root.getId())).thenReturn(true);

        assertThatThrownBy(() -> wbsService.deleteWbsNode(root.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete WBS node that has child nodes.");
    }
}
