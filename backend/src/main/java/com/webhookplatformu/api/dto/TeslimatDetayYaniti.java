package com.webhookplatformu.api.dto;

import java.util.List;

public record TeslimatDetayYaniti(TeslimatOzetiYaniti teslimat, List<TeslimatDenemesiYaniti> denemeler,
                                   MotorGorevOzetiYaniti motorGorevOzeti) {
}
