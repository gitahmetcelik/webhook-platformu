package com.webhookplatformu.api.dto;

import java.util.List;

public record TeslimatDetayYaniti(TeslimatOzetiYaniti teslimat, String olayTipi, String olayPayload,
                                   String endpointUrl, List<TeslimatDenemesiYaniti> denemeler,
                                   MotorGorevOzetiYaniti motorGorevOzeti) {
}
