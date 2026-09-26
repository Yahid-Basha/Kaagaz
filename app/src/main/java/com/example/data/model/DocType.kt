package com.example.data.model

enum class DocType(
    val displayName: String,
    val code: String,
    val description: String
) {
    RC(
        displayName = "Registration Certificate (RC)",
        code = "RC",
        description = "Vehicle registration & ownership certificate"
    ),
    PUC(
        displayName = "Pollution Under Control (PUC)",
        code = "PUC",
        description = "Vehicle emission compliance certificate"
    ),
    LPG_BILL(
        displayName = "LPG Gas Connection Bill",
        code = "LPG",
        description = "Domestic cylinder supply & subsidy link"
    ),
    PAN_CARD(
        displayName = "Permanent Account Number (PAN)",
        code = "PAN",
        description = "Income Tax Department identity card"
    ),
    INSURANCE(
        displayName = "Insurance Policy Document",
        code = "INSURANCE",
        description = "Motor, Health or Term Life insurance policy"
    )
}
