package ee.carlrobert.codegpt.inlineedit.engine

import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ModelSelectionService
import ee.carlrobert.codegpt.settings.service.ServiceType

object InlineEditApplyStrategyFactory {
    fun get(): ApplyStrategy {
        return SearchReplaceApplyStrategy()
    }
}

