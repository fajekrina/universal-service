package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiScript;
import com.fajekrina.universal.model.entity.SduiTree;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.SaveDraftPageRequestBuilder;
import com.fajekrina.universal.model.response.DraftPageResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiScriptRepository;
import com.fajekrina.universal.repository.SduiTreeRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveDraftPageServiceTest {

    @InjectMocks
    SaveDraftPageService saveDraftPageService;

    @Mock SduiPageRepository pageRepository;
    @Mock SduiTreeRepository treeRepository;
    @Mock SduiScriptRepository scriptRepository;
    @Mock MerchantHelperService merchantHelperService;
    @Mock ValidationHelperService validationHelperService;
    @Mock DraftHelperService draftHelperService;
    @Mock AuditHelperService auditHelperService;

    private static Map<String, Object> layout() {
        return Map.of("type", "Container", "components", List.of(
                Map.of("id", "01HQ0000000000000000000001", "component", "Button", "props",
                        Map.of("label", "Go", "action", Map.of("behavior", "navigate", "target", "home")))));
    }

    @Test
    void splitsTheMonolithIntoTreeAndScriptArtefacts() {
        SduiPage page = SduiPage.builder().pageId("m-home").merchantId("m").pageKey("home").status(PageStatus.DRAFT).build();
        page.setVersion(0);
        when(pageRepository.findByMerchantIdAndPageKey("m", "home")).thenReturn(Optional.of(page));
        when(treeRepository.findByPageIdAndContentHash(eq("m-home"), anyString())).thenReturn(Optional.empty());
        when(treeRepository.findFirstByPageIdOrderByVersionDesc("m-home")).thenReturn(Optional.empty());
        when(treeRepository.saveAndFlush(any())).thenAnswer(i -> { SduiTree t = i.getArgument(0); t.setTreeId(10L); return t; });
        when(scriptRepository.findByPageIdAndContentHash(eq("m-home"), anyString())).thenReturn(Optional.empty());
        when(scriptRepository.findFirstByPageIdOrderByVersionDesc("m-home")).thenReturn(Optional.empty());
        when(scriptRepository.saveAndFlush(any())).thenAnswer(i -> { SduiScript s = i.getArgument(0); s.setScriptId(20L); return s; });
        when(pageRepository.saveAndFlush(page)).thenReturn(page);
        when(draftHelperService.toDraftResponse(page)).thenReturn(DraftPageResponseBuilder.builder().lockVersion(1).build());

        var res = saveDraftPageService.execute(SaveDraftPageRequestBuilder.builder()
                .merchantId("m").pageKey("home").layout(layout()).build());

        assertThat(res.tree()).isEqualTo("home@1");
        assertThat(res.script()).isEqualTo("home-logic@1");
        assertThat(res.unchanged()).isEmpty();
        assertThat(page.getDraftTreeId()).isEqualTo(10L);
        ArgumentCaptor<SduiScript> script = ArgumentCaptor.forClass(SduiScript.class);
        verify(scriptRepository).saveAndFlush(script.capture());
        assertThat(script.getValue().getBody()).contains("\"behavior\":\"navigate\"");
        assertThat(script.getValue().getActionCount()).isEqualTo(1);
        verify(validationHelperService).validateOrThrow(eq(layout()), any());
    }

    @Test
    void staleLockVersionIsAConflict() {
        SduiPage page = SduiPage.builder().pageId("m-home").merchantId("m").pageKey("home").status(PageStatus.DRAFT).build();
        page.setVersion(5);
        when(pageRepository.findByMerchantIdAndPageKey("m", "home")).thenReturn(Optional.of(page));
        assertThatThrownBy(() -> saveDraftPageService.execute(SaveDraftPageRequestBuilder.builder()
                .merchantId("m").pageKey("home").layout(layout()).lockVersion(4).build()))
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.LOCK_CONFLICT));
        verify(treeRepository, never()).saveAndFlush(any());
    }

    @Test
    void layoutIsRequiredAndSchemaVersionMustBe1x() {
        assertThatThrownBy(() -> saveDraftPageService.execute(SaveDraftPageRequestBuilder.builder().merchantId("m").pageKey("home").build()))
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.INVALID_PAYLOAD));
        assertThat(SaveDraftPageService.normalizeSchemaVersion("1.02")).isEqualTo("1.2");
        assertThatThrownBy(() -> SaveDraftPageService.normalizeSchemaVersion("2.0")).isInstanceOf(UniversalException.class);
    }
}
