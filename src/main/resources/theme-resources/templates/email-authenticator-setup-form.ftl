<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=message?has_content; section>
    <#if section="header">
        ${msg("email-authenticator-setup-title")}
    <#elseif section="form">
        <form id="kc-email-authenticator-setup-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="credentialId" value="">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <p>
                        ${msg("email-authenticator-setup-description")}
                    </p>
                </div>
            </div>
            <#if message?has_content>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcAlertClass!} ${properties.kcAlertErrorClass!}">
                        <div class="${properties.kcAlertIconClass!}">
                            <span class="${properties.kcFeedbackErrorIcon!}"></span>
                        </div>
                        <div class="${properties.kcAlertMessageClass!}">
                            ${kcSanitize(message.summary)?no_esc}
                        </div>
                    </div>
                </div>
            </#if>
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("email-authenticator-setup-button")}" />
                        <#if isAppInitiatedAction??>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" type="submit" value="${msg("doCancel")}" />
                        </#if>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>