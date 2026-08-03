import re

with open('be/src/main/java/com/bemo/hr/shared/api/ApiExceptionHandler.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_bre = '''    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessConflict(BusinessRuleException exception, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = exception.getFields().stream()
                .map(field -> new ApiError.FieldError(field, exception.getCode(), exception.getMessage()))
                .toList();
        return respond(exception.getCode(), exception.getStatus(), raw(exception.getMessage()), request, fieldErrors);
    }'''

new_bre = '''    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessConflict(BusinessRuleException exception, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = exception.getFields().stream()
                .map(field -> new ApiError.FieldError(field, exception.getCode(), translationService.translateOrDefault(exception.getCode(), resolveLocale(request), exception.getMessage())))
                .toList();
        return respond(exception.getCode(), exception.getStatus(), translated(exception.getCode(), resolveLocale(request), exception.getMessage()), request, fieldErrors);
    }'''

content = content.replace(old_bre, new_bre)

old_translated = '''    private ErrorText translated(String key, String locale) {
        return new ErrorText(
                translationService.translateOrDefault(key, "en-US", key),
                translationService.translateOrDefault(key, locale, key));
    }'''

new_translated = '''    private ErrorText translated(String key, String locale) {
        return new ErrorText(
                translationService.translateOrDefault(key, "en-US", key),
                translationService.translateOrDefault(key, locale, key));
    }

    private ErrorText translated(String key, String locale, String defaultMsg) {
        return new ErrorText(
                translationService.translateOrDefault(key, "en-US", defaultMsg),
                translationService.translateOrDefault(key, locale, defaultMsg));
    }'''

content = content.replace(old_translated, new_translated)

with open('be/src/main/java/com/bemo/hr/shared/api/ApiExceptionHandler.java', 'w', encoding='utf-8') as f:
    f.write(content)
