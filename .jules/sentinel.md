## 2026-01-11 - Default Password in Config
**Vulnerability:** Found default password `123456` in `application-dev.yml`.
**Learning:** Default passwords in configs, even dev ones, can lead to accidental exposure if configs are copied or if the same password is used elsewhere.
**Prevention:** Always use environment variables without default values or with placeholder values that force explicit configuration.

## 2026-01-11 - Actuator Endpoint Exposure
**Vulnerability:** `management.endpoints.web.exposure.include` in `application-dev.yml` includes `env` and `beans`.
**Learning:** Exposing `env` can leak environment variables (secrets). Exposing `beans` reveals internal application structure.
**Prevention:** Limit exposed actuator endpoints to `health` and `info` by default. Enable sensitive ones only when strictly necessary and behind authentication.

## 2026-01-11 - Missing Input Validation on MedicalRecord
**Vulnerability:** `MedicalRecordDTO` has loose validation. While `@Schema` describes fields, there are few JSR-380 (Bean Validation) annotations.
**Learning:** Relying on documentation or "suggestions" for input length and content is insufficient. Large inputs can cause DoS or DB errors.
**Prevention:** Use `@Size`, `@NotBlank`, and other validation annotations on DTOs to enforce constraints at the controller level.
