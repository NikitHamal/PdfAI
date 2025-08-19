package com.pdf.ai.ui.interaction;

import com.pdf.ai.OutlineData;

public interface OnOutlineActionListener {
    void onApproveOutline(OutlineData outlineData);
    void onDiscardOutline(int position);
}
