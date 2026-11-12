package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.PageKeyRequestBuilder;
import com.fajekrina.universal.model.response.ReleaseResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishPageServiceTest {

    @InjectMocks
    PublishPageService publishPageService;

    @Mock
    SduiPageRepository pageRepository;

    @Mock
    ReleaseHelperService releaseHelperService;

    private static SduiPage page(Long treeId) {
        return SduiPage.builder().pageId("m-home").merchantId("m").pageKey("home").status(PageStatus.DRAFT)
                .draftTreeId(treeId).draftScriptId(2L).draftStyleId(null).build();
    }

    @Test
    void publishesTheDraftArtefactsUnderARowLock() {
        SduiPage page = page(1L);
        when(pageRepository.findByMerchantIdAndPageKey("m", "home")).thenReturn(Optional.of(page));
        when(pageRepository.findForUpdate("m-home")).thenReturn(Optional.of(page));
        when(releaseHelperService.createRelease(page, 1L, null, 2L, null))
                .thenReturn(ReleaseResponseBuilder.builder().releaseId("m:home@1").sequence(1).build());

        var res = publishPageService.execute(PageKeyRequestBuilder.builder().merchantId("m").pageKey("home").build());

        assertThat(res.releaseId()).isEqualTo("m:home@1");
        verify(pageRepository).findForUpdate("m-home");
    }

    @Test
    void unknownPageIsNotFound() {
        when(pageRepository.findByMerchantIdAndPageKey("m", "nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> publishPageService.execute(PageKeyRequestBuilder.builder().merchantId("m").pageKey("nope").build()))
                .isInstanceOf(UniversalException.class)
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.PAGE_NOT_FOUND));
        verifyNoInteractions(releaseHelperService);
    }

    @Test
    void pageWithoutDraftCannotBePublished() {
        SduiPage page = page(null);
        when(pageRepository.findByMerchantIdAndPageKey("m", "home")).thenReturn(Optional.of(page));
        when(pageRepository.findForUpdate("m-home")).thenReturn(Optional.of(page));
        assertThatThrownBy(() -> publishPageService.execute(PageKeyRequestBuilder.builder().merchantId("m").pageKey("home").build()))
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.NO_DRAFT));
    }
}
