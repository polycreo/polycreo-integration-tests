package org.polycreo.tests.integration

interface AbstractIdentifyIntegrationTest<T : Any> {

    companion object {
        val ID_ANNOTATION_NAME: Collection<String> = listOf(
                "javax.persistence.Id",
                "org.springframework.data.annotation.Id"
            )
    }

    val T.id: Any?
        get() {
            var c: Class<*> = this.javaClass
            while (c != Any::class.java) {
                val declaredFields = c.declaredFields
                for (field in declaredFields) {
                    for (annotation in field.annotations) {
                        if (ID_ANNOTATION_NAME.contains(annotation.annotationClass.java.name)) {
                            field.isAccessible = true
                            try {
                                @Suppress("UNCHECKED_CAST")
                                return field[this] as T
                            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                                // NOPMD ignore
                            }
                        }
                    }
                }
                c = c.superclass
            }
            return null
        }
}
