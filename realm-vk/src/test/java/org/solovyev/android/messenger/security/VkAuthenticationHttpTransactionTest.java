package org.solovyev.android.messenger.security;

import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.solovyev.android.messenger.vk.VkConfigurationImpl;

/**
 * User: serso
 * Date: 5/28/12
 * Time: 10:34 PM
 */
public class VkAuthenticationHttpTransactionTest {

    @Nonnull
    private static final String CLIENT_ID = "2970921";

    @Nonnull
    private static final String CLIENT_SECRET = "Scm7M1vxOdDjpeVj81jw";

    @Before
    public void setUp() throws Exception {
        VkConfigurationImpl.getInstance().setClientId(CLIENT_ID);
        VkConfigurationImpl.getInstance().setClientSecret(CLIENT_SECRET);
    }

    @Test
    public void testErrorResult() throws Exception {
        /*try {
            HttpTransactions.execute(new VkAuthenticationHttpTransaction("test", "test"));
            Assert.fail();
        } catch (VkResponseErrorException e) {
            final CommonApiError vkError = e.getApiError();
            // just to be sure that we've got known error
            final VkErrorType vkErrorType = VkErrorType.valueOf(vkError.getErrorId());

            switch (vkErrorType) {
                case invalid_client:
                    break;
                case need_captcha:
                    final Captcha captcha = vkError.getCaptcha();
                    Assert.assertNotNull(captcha);
                    Assert.assertNotNull(captcha.getCaptchaSid());
                    Assert.assertNotNull(captcha.getCaptchaImage());
                    break;
            }
        }*/
    }
}