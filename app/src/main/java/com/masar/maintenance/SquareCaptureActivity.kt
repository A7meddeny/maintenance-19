package com.masar.maintenance

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.Size

/** نشاط مسح QR بنافذة مربعة (بدل المستطيلة الافتراضية). */
class SquareCaptureActivity : ComponentActivity() {

    private lateinit var capture: CaptureManager
    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        barcodeView = DecoratedBarcodeView(this)
        // نافذة مسح مربعة
        barcodeView.barcodeView.setFramingRectSize(Size(720, 720))
        barcodeView.setStatusText("وجّه الكاميرا نحو رمز السيارة")
        setContentView(barcodeView)

        capture = CaptureManager(this, barcodeView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()
    }

    override fun onResume() { super.onResume(); capture.onResume() }
    override fun onPause() { super.onPause(); capture.onPause() }
    override fun onDestroy() { super.onDestroy(); capture.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); capture.onSaveInstanceState(outState) }
}
