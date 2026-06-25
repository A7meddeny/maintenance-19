package com.masar.maintenance.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class Repository {

    private val gson = Gson()
    private val session get() = Net.session

    /* ============ مساعدات منخفضة المستوى ============ */

    private suspend fun callGet(file: String, params: Map<String, String>): Outcome<JsonObject> {
        return try {
            val r = Net.api().get(file, params)
            val b = r.body()
            when {
                r.isSuccessful && b != null && b.get("ok")?.asBoolean == true -> Outcome.Ok(b)
                b != null && b.has("error") -> Outcome.Err(b.get("error").asString)
                else -> Outcome.Err(errBody(r) ?: "خطأ في الاتصال (${r.code()})")
            }
        } catch (e: Exception) { Outcome.Err(netMsg(e)) }
    }

    private suspend fun callPost(
        file: String,
        fields: Map<String, String?>,
        files: List<MultipartBody.Part> = emptyList()
    ): Outcome<JsonObject> {
        return try {
            val parts = fields.filterValues { it != null }
                .mapValues { (it.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull()) }
            val r = Net.api().postMultipart(file, parts, files)
            val b = r.body()
            when {
                r.isSuccessful && b != null && b.get("ok")?.asBoolean == true -> Outcome.Ok(b)
                b != null && b.has("error") -> Outcome.Err(b.get("error").asString)
                else -> Outcome.Err(errBody(r) ?: "خطأ في الاتصال (${r.code()})")
            }
        } catch (e: Exception) { Outcome.Err(netMsg(e)) }
    }

    /** يقرأ رسالة الخطأ من جسم الأخطاء (للأكواد غير 2xx مثل 409) */
    private fun errBody(r: retrofit2.Response<JsonObject>): String? = try {
        val s = r.errorBody()?.string()
        if (s.isNullOrBlank()) null
        else gson.fromJson(s, JsonObject::class.java)?.takeIf { it.has("error") }?.get("error")?.asString
    } catch (e: Exception) { null }

    private fun netMsg(e: Exception): String =
        "تعذّر الاتصال بالخادم. تحقّق من الرابط والإنترنت." + (e.message?.let { "\n($it)" } ?: "")

    private fun part(field: String, uf: UploadFile?): MultipartBody.Part? {
        if (uf == null) return null
        val body = uf.file.asRequestBody(uf.mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(field, uf.fileName, body)
    }

    private fun dataArray(o: JsonObject): JsonArray =
        if (o.has("data") && o.get("data").isJsonArray) o.getAsJsonArray("data") else JsonArray()

    /* ============ المصادقة ============ */

    suspend fun login(baseUrl: String, username: String, password: String, device: String): Outcome<User> {
        session.baseUrl = baseUrl
        return try {
            val r = Net.api().login(
                mapOf("action" to "login", "username" to username, "password" to password,
                      "client" to "app", "device" to device)
            )
            val b = r.body()
            if (r.isSuccessful && b != null && b.get("ok")?.asBoolean == true) {
                session.token = b.get("token")?.asString
                val user = gson.fromJson(b.getAsJsonObject("user"), User::class.java)
                session.saveUser(user)
                Outcome.Ok(user)
            } else {
                Outcome.Err(b?.get("error")?.asString ?: "فشل تسجيل الدخول")
            }
        } catch (e: Exception) { Outcome.Err(netMsg(e)) }
    }

    suspend fun me(): Outcome<User> {
        val o = callGet("auth.php", mapOf("action" to "me"))
        return when (o) {
            is Outcome.Ok -> Outcome.Ok(gson.fromJson(o.data.getAsJsonObject("user"), User::class.java))
            is Outcome.Err -> o
        }
    }

    suspend fun logout() {
        try { callPost("auth.php", mapOf("action" to "logout")) } catch (_: Exception) {}
        session.clearAuth()
    }

    /* ============ السيارات ============ */

    suspend fun cars(q: String = "", state: String = ""): Outcome<List<Car>> {
        val o = callGet("cars.php", mapOf("action" to "list", "q" to q, "state" to state))
        return map(o) { gson.fromJson(dataArray(it), Array<Car>::class.java).toList() }
    }

    suspend fun car(id: Int): Outcome<Car> {
        val o = callGet("cars.php", mapOf("action" to "get", "id" to id.toString()))
        return map(o) { gson.fromJson(it.getAsJsonObject("data"), Car::class.java) }
    }

    suspend fun carByQr(token: String): Outcome<Car> {
        val o = callGet("cars.php", mapOf("action" to "get_by_qr", "qr" to token))
        return map(o) { gson.fromJson(it.getAsJsonObject("data"), Car::class.java) }
    }

    /* ===== تقارير الفحص ===== */
    suspend fun inspections(status: String = "", carId: Int = 0): Outcome<List<InspectionRow>> {
        val params = mutableMapOf("action" to "list")
        if (status.isNotBlank()) params["status"] = status
        if (carId > 0) params["car_id"] = carId.toString()
        val o = callGet("inspections.php", params)
        return map(o) { gson.fromJson(dataArray(it), Array<InspectionRow>::class.java).toList() }
    }

    suspend fun inspectionGet(id: Int): Outcome<InspectionDetail> {
        val o = callGet("inspections.php", mapOf("action" to "get", "id" to id.toString()))
        return map(o) { gson.fromJson(it.getAsJsonObject("data"), InspectionDetail::class.java) }
    }

    suspend fun inspectionSubmit(id: Int, dataJson: String, photos: List<UploadFile>): Outcome<Unit> {
        val parts = photos.mapIndexedNotNull { i, uf -> part("photo_$i", uf) }
        val o = callPost("inspections.php", mapOf("action" to "submit", "id" to id.toString(), "data_json" to dataJson), parts)
        return map(o) { }
    }

    /* ============ التأجير ============ */
    suspend fun rentalAvailable(q: String = ""): Outcome<List<Car>> {
        val o = callGet("rentals.php", mapOf("action" to "available", "q" to q))
        return map(o) { gson.fromJson(dataArray(it), Array<Car>::class.java).toList() }
    }

    suspend fun followEmployees(): Outcome<List<FollowEmployee>> {
        val o = callGet("rentals.php", mapOf("action" to "follow_employees"))
        return map(o) { gson.fromJson(dataArray(it), Array<FollowEmployee>::class.java).toList() }
    }

    suspend fun rentalCreate(
        carId: Int, renter: String, phone: String, days: Int, agreedPrice: String, contract: String, followId: Int
    ): Outcome<Int> {
        val o = callPost("rentals.php", mapOf(
            "action" to "create", "car_id" to carId.toString(), "renter_name" to renter,
            "renter_phone" to phone, "days" to days.toString(), "agreed_price" to agreedPrice,
            "contract_no" to contract, "follow_user_id" to followId.toString()
        ))
        return map(o) { it.get("id")?.asInt ?: 0 }
    }

    suspend fun myRentals(): Outcome<List<Rental>> {
        val o = callGet("rentals.php", mapOf("action" to "my_rentals"))
        return map(o) { gson.fromJson(dataArray(it), Array<Rental>::class.java).toList() }
    }

    suspend fun rentalGet(id: Int): Outcome<Rental> {
        val o = callGet("rentals.php", mapOf("action" to "get", "id" to id.toString()))
        return map(o) { gson.fromJson(it.getAsJsonObject("data"), Rental::class.java) }
    }

    suspend fun rentalSubmitInspection(
        rentalId: Int, kind: String, checklistJson: String, bodyJson: String,
        km: String, note: String, hasIssues: Boolean, photos: List<UploadFile>
    ): Outcome<Unit> {
        val parts = photos.mapIndexedNotNull { i, uf -> part("photo_$i", uf) }
        val o = callPost("rentals.php", mapOf(
            "action" to "submit_inspection", "rental_id" to rentalId.toString(), "kind" to kind,
            "checklist" to checklistJson, "body_points" to bodyJson, "km" to km, "note" to note,
            "has_issues" to (if (hasIssues) "1" else "0")
        ), parts)
        return map(o) { }
    }

    /* ===== إشعارات FCM ===== */
    suspend fun registerDevice(fcmToken: String): Outcome<Unit> {
        val o = callGet("devices.php", mapOf("action" to "register", "fcm_token" to fcmToken))
        return map(o) { }
    }

    suspend fun unregisterDevice(fcmToken: String): Outcome<Unit> {
        val o = callGet("devices.php", mapOf("action" to "unregister", "fcm_token" to fcmToken))
        return map(o) { }
    }

    suspend fun carHistory(id: Int): Outcome<Pair<Car?, List<RequestDetail>>> {
        val o = callGet("cars.php", mapOf("action" to "history", "id" to id.toString()))
        return map(o) {
            val car = if (it.has("car") && it.get("car").isJsonObject)
                gson.fromJson(it.getAsJsonObject("car"), Car::class.java) else null
            val reqs = if (it.has("requests") && it.get("requests").isJsonArray)
                gson.fromJson(it.getAsJsonArray("requests"), Array<RequestDetail>::class.java).toList()
            else emptyList()
            car to reqs
        }
    }

    suspend fun branches(q: String = ""): Outcome<List<Branch>> {
        val o = callGet("branches.php", mapOf("action" to "list", "q" to q))
        return map(o) { gson.fromJson(dataArray(it), Array<Branch>::class.java).toList() }
    }

    suspend fun rentalCompanies(q: String = ""): Outcome<List<RentalCompany>> {
        val o = callGet("rental_companies.php", mapOf("action" to "list", "q" to q))
        return map(o) { gson.fromJson(dataArray(it), Array<RentalCompany>::class.java).toList() }
    }

    suspend fun myTasks(status: String = "pending"): Outcome<List<MaintTask>> {
        val o = callGet("tasks.php", mapOf("action" to "mine", "status" to status))
        return map(o) { gson.fromJson(dataArray(it), Array<MaintTask>::class.java).toList() }
    }

    suspend fun taskComplete(id: Int, doneNote: String, donePhoto: UploadFile?): Outcome<Unit> {
        val parts = buildList { part("done_photo", donePhoto)?.let { add(it) } }
        return map(callPost("tasks.php", mapOf("action" to "complete", "id" to id.toString(), "done_note" to doneNote), parts)) { }
    }

    suspend fun dailyWorksList(userId: Int = 0, from: String = "", to: String = ""): Outcome<List<DailyWork>> {
        val o = callGet("daily_works.php", mapOf("action" to "list", "user_id" to userId.toString(), "date_from" to from, "date_to" to to))
        return map(o) { gson.fromJson(dataArray(it), Array<DailyWork>::class.java).toList() }
    }

    suspend fun dailyWorkCreate(
        fields: Map<String, String?>,
        mainPhoto: UploadFile?,
        carPhotos: List<UploadFile?> = emptyList(),
        driverPhotos: List<UploadFile?> = emptyList(),
        handoverPhotos: List<UploadFile?> = emptyList()
    ): Outcome<Unit> {
        val parts = buildList {
            part("photo", mainPhoto)?.let { add(it) }
            carPhotos.forEachIndexed { i, uf -> part("carphoto_$i", uf)?.let { add(it) } }
            driverPhotos.forEachIndexed { i, uf -> part("driverphoto_$i", uf)?.let { add(it) } }
            handoverPhotos.forEachIndexed { i, uf -> part("handover_$i", uf)?.let { add(it) } }
        }
        return map(callPost("daily_works.php", fields, parts)) { }
    }

    suspend fun saveCar(fields: Map<String, String?>, photo: UploadFile?, plate: UploadFile?, reg: UploadFile?): Outcome<JsonObject> {
        val parts = listOfNotNull(part("photo", photo), part("plate_photo", plate), part("registration_photo", reg))
        return callPost("cars.php", fields, parts)
    }

    suspend fun updateOdometer(id: Int, odometer: String): Outcome<JsonObject> =
        callPost("cars.php", mapOf("action" to "update_odometer", "id" to id.toString(), "odometer" to odometer))

    /* ============ الإشعارات (الجرس) ============ */
    suspend fun notifications(): Outcome<List<Noti>> {
        val o = callGet("notifications.php", mapOf("action" to "list"))
        return map(o) { gson.fromJson(dataArray(it), Array<Noti>::class.java).toList() }
    }

    suspend fun deleteCar(id: Int): Outcome<JsonObject> =
        callPost("cars.php", mapOf("action" to "delete", "id" to id.toString()))

    /* ============ الشركات ============ */

    suspend fun companies(q: String = ""): Outcome<List<Company>> {
        val o = callGet("companies.php", mapOf("action" to "list", "q" to q))
        return map(o) { gson.fromJson(dataArray(it), Array<Company>::class.java).toList() }
    }

    suspend fun saveCompany(fields: Map<String, String?>, logo: UploadFile?): Outcome<JsonObject> =
        callPost("companies.php", fields, listOfNotNull(part("logo", logo)))

    suspend fun deleteCompany(id: Int): Outcome<JsonObject> =
        callPost("companies.php", mapOf("action" to "delete", "id" to id.toString()))

    /* ============ الموظفون ============ */

    suspend fun employees(q: String = ""): Outcome<List<Employee>> {
        val o = callGet("employees.php", mapOf("action" to "list", "q" to q))
        return map(o) { gson.fromJson(dataArray(it), Array<Employee>::class.java).toList() }
    }

    suspend fun saveEmployee(fields: Map<String, String?>, photo: UploadFile?): Outcome<JsonObject> =
        callPost("employees.php", fields, listOfNotNull(part("photo", photo)))

    suspend fun deleteEmployee(id: Int): Outcome<JsonObject> =
        callPost("employees.php", mapOf("action" to "delete", "id" to id.toString()))

    suspend fun staffFollowup(): Outcome<List<StaffMember>> {
        val o = callGet("employees.php", mapOf("action" to "staff_followup"))
        return map(o) { gson.fromJson(dataArray(it), Array<StaffMember>::class.java).toList() }
    }

    /** قائمة موظفين مختصرة (لأي دور) للإسناد */
    suspend fun staff(role: String): Outcome<List<User>> {
        val o = callGet("staff.php", mapOf("action" to "list", "role" to role))
        return map(o) { gson.fromJson(dataArray(it), Array<User>::class.java).toList() }
    }

    /* ============ الطلبات ============ */

    suspend fun requests(
        q: String = "", status: String = "", scope: String = "",
        staffId: Int = 0, staffRole: String = "", type: String = "", excludePeriodic: Boolean = false
    ): Outcome<List<RequestRow>> {
        val p = mutableMapOf("action" to "list", "q" to q, "status" to status, "scope" to scope)
        if (staffId > 0) { p["staff_id"] = staffId.toString(); p["staff_role"] = staffRole }
        if (type.isNotEmpty()) p["type"] = type
        if (excludePeriodic) p["exclude_periodic"] = "1"
        val o = callGet("requests.php", p)
        return map(o) { gson.fromJson(dataArray(it), Array<RequestRow>::class.java).toList() }
    }

    suspend fun requestDetail(id: Int): Outcome<RequestDetail> {
        val o = callGet("requests.php", mapOf("action" to "get", "id" to id.toString()))
        return map(o) { gson.fromJson(it.getAsJsonObject("data"), RequestDetail::class.java) }
    }

    suspend fun kpis(): Outcome<Kpis> {
        val o = callGet("dashboard.php", mapOf("action" to "kpis"))
        return map(o) { gson.fromJson(it.getAsJsonObject("data"), Kpis::class.java) }
    }

    /* ============ إجراءات دورة حياة الطلب ============ */

    suspend fun officeCreate(carId: Int, maintId: Int, desc: String, odometer: String, photo: UploadFile?): Outcome<JsonObject> =
        callPost("requests.php", mapOf(
            "action" to "office_create", "car_id" to carId.toString(),
            "maintenance_user_id" to maintId.toString(), "problem_description" to desc,
            "odometer_in" to odometer
        ), listOfNotNull(part("problem_photo", photo)))

    suspend fun periodicSelf(carId: Int): Outcome<JsonObject> =
        callPost("requests.php", mapOf("action" to "periodic_self", "car_id" to carId.toString()))

    /** المكتب/الإدارة: فتح صيانة دورية وإسنادها لموظف صيانة */
    suspend fun periodicAssign(carId: Int, maintId: Int): Outcome<JsonObject> =
        callPost("requests.php", mapOf("action" to "periodic_assign",
            "car_id" to carId.toString(), "maintenance_user_id" to maintId.toString()))

    /** إغلاق صيانة دورية: بنود (مورد/فاتورة/مبلغ) + عداد + ملاحظة + صور */
    suspend fun periodicComplete(
        id: Int, itemsJson: String, odometerOut: String, note: String,
        files: List<MultipartBody.Part>
    ): Outcome<JsonObject> =
        callPost("requests.php", mapOf(
            "action" to "periodic_complete", "id" to id.toString(),
            "items" to itemsJson, "odometer_out" to odometerOut, "completion_note" to note
        ), files)

    suspend fun maintenanceReceive(id: Int) = simple("maintenance_receive", id)

    suspend fun maintenanceItems(id: Int, itemsJson: String, itemPhotos: Map<String, UploadFile>): Outcome<JsonObject> {
        val parts = itemPhotos.mapNotNull { (k, v) -> part(k, v) }
        return callPost("requests.php", mapOf("action" to "maintenance_items", "id" to id.toString(), "items" to itemsJson), parts)
    }

    suspend fun purchasingReceive(id: Int) = simple("purchasing_receive", id)

    suspend fun purchasingPrice(id: Int, pricesJson: String, quoteParts: List<MultipartBody.Part>, extra: Map<String, String?>): Outcome<JsonObject> {
        val f = mutableMapOf<String, String?>("action" to "purchasing_price", "id" to id.toString(), "prices" to pricesJson)
        f.putAll(extra)
        return callPost("requests.php", f, quoteParts)
    }

    suspend fun adminApprove(id: Int, approvalsJson: String, note: String): Outcome<JsonObject> =
        callPost("requests.php", mapOf("action" to "admin_approve", "id" to id.toString(), "approvals" to approvalsJson, "note" to note))

    /** الإدارة: رفض الطلب وإعادته للمشتريات بسبب */
    suspend fun adminReject(id: Int, reason: String): Outcome<JsonObject> =
        callPost("requests.php", mapOf("action" to "admin_reject", "id" to id.toString(), "reason" to reason))

    suspend fun purchasingBuy(id: Int) = simple("purchasing_buy", id)
    suspend fun purchasingDeliver(id: Int) = simple("purchasing_deliver", id)

    /** المشتريات: تسليم القطعة مع تحديد الفرع لكل بند وصورة اختيارية */
    suspend fun purchasingDeliverFull(id: Int, branchesJson: String, purchasingPhoto: UploadFile?): Outcome<JsonObject> =
        callPost("requests.php", mapOf("action" to "purchasing_deliver", "id" to id.toString(), "branches" to branchesJson),
            listOfNotNull(part("purchasing_photo", purchasingPhoto)))

    suspend fun maintenancePart(id: Int) = simple("maintenance_part", id)

    /** إنهاء صيانة عامة: عداد + ملاحظة + صورة اختيارية */
    suspend fun maintenanceComplete(id: Int, odometerOut: String, note: String = "", photo: UploadFile? = null): Outcome<JsonObject> =
        callPost("requests.php", mapOf(
            "action" to "maintenance_complete", "id" to id.toString(),
            "odometer_out" to odometerOut, "completion_note" to note
        ), listOfNotNull(part("completion_photo", photo)))

    /* ============ كشف حساب الموردين ============ */
    suspend fun supplierLedger(companyId: Int, scope: String = "all", q: String = ""): Outcome<JsonObject> =
        callGet("suppliers.php", mapOf("action" to "ledger", "company_id" to companyId.toString(), "scope" to scope, "q" to q))

    private suspend fun simple(action: String, id: Int): Outcome<JsonObject> =
        callPost("requests.php", mapOf("action" to action, "id" to id.toString()))

    /** بناء جزء ملف عرض سعر للرفع */
    fun quotePart(field: String, uf: UploadFile): MultipartBody.Part = part(field, uf)!!

    /* ============ أداة تحويل النتيجة ============ */
    private inline fun <T> map(o: Outcome<JsonObject>, transform: (JsonObject) -> T): Outcome<T> =
        when (o) {
            is Outcome.Ok -> try { Outcome.Ok(transform(o.data)) } catch (e: Exception) { Outcome.Err("تعذّر قراءة البيانات") }
            is Outcome.Err -> o
        }
}
