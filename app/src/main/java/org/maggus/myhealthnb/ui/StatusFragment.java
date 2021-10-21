package org.maggus.myhealthnb.ui;

import android.text.Html;
import android.widget.TextView;

import org.maggus.myhealthnb.MainActivity;
import org.maggus.myhealthnb.R;

import androidx.fragment.app.Fragment;

public abstract class StatusFragment extends Fragment {
    protected TextView textView;

    protected enum Status {
        Success,
        Note,
        Warning,
        Error
    }

    protected void formatStatusText(String text) {
        formatStatusText(text, Status.Note);
    }

    protected void formatStatusText(String text, Status status) {
        formatStatusText(text, status, textView);
    }

    protected void formatStatusText(String text, Status status, TextView textViewCtrl) {
        if (text == null || text.isEmpty()) {
            textViewCtrl.setText("");
        }
        if (status == Status.Error) {
            setHtmlText("<h2><font color='" + colorFromRes(R.color.error_bg) + "'>" + text + "</font></h2>", textViewCtrl);
        } else if (status == Status.Warning) {
            setHtmlText("<h3><font color='" + colorFromRes(R.color.error_info_bg) + "'>" + text + "</font></h3>", textViewCtrl);
        } else if (status == Status.Note) {
            setHtmlText("<h3><font color='" + colorFromRes(R.color.black) + "'>" + text + "</font></h3>", textViewCtrl);
        } else if (status == Status.Success) {
            setHtmlText("<h2><font color='" + colorFromRes(R.color.success_bg) + "'>" + text + "</font></h2>", textViewCtrl);
        } else {
            textViewCtrl.setText(text);
        }
    }

    protected void setHtmlText(String html) {
        setHtmlText(html, textView);
    }

    protected void setHtmlText(String html, TextView textViewCtrl) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textViewCtrl.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } else {
            textViewCtrl.setText(Html.fromHtml(html));
        }
    }

    protected String colorFromRes(int id) {
        int color = getResources().getColor(id);
        return "#" + String.format("%X", color).substring(2); // !!strip alpha value!!
    }

    protected MainActivity getMainActivity() {
        return ((MainActivity) getActivity());
    }
}
