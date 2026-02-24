<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('emailCode'); section>
    <#if section="header">
        ${msg("email-authenticator-setup-verify-title")}
    <#elseif section="form">
        <form id="kc-email-authenticator-setup-verify-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
            method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="emailCode" class="${properties.kcLabelClass!}">${msg("email-authenticator-setup-verify-description")}</label>
                </div>

            <div class="${properties.kcInputWrapperClass!}">
                <input id="emailCode" name="emailCode" autocomplete="off" type="text" class="${properties.kcInputClass!}"
                       autofocus aria-invalid="<#if messagesPerField.existsError('emailCode')>true</#if>"
                       <#if maxAttemptsReached?? && maxAttemptsReached>disabled</#if>/>

                <#if messagesPerField.existsError('emailCode')>
                    <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}"
                          aria-live="polite">
                        ${kcSanitize(messagesPerField.get('emailCode'))?no_esc}
                    </span>
                </#if>
            </div>
        </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <#if !(maxAttemptsReached?? && maxAttemptsReached)>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("email-authenticator-setup-verify-button")}" />
                        </#if>
                        <input class="${properties.kcButtonClass!} <#if maxAttemptsReached?? && maxAttemptsReached>${properties.kcButtonPrimaryClass!}<#else>${properties.kcButtonDefaultClass!}</#if> ${properties.kcButtonLargeClass!}" name="resend" type="submit" value="${msg("resendCode")}"/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" type="submit" value="${msg("doCancel")}"/>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
