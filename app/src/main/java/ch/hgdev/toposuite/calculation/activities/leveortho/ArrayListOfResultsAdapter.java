package ch.hgdev.toposuite.calculation.activities.leveortho;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import ch.hgdev.toposuite.R;
import ch.hgdev.toposuite.calculation.LeveOrthogonal;
import ch.hgdev.toposuite.utils.DisplayUtils;

public class ArrayListOfResultsAdapter extends ArrayAdapter<LeveOrthogonal.Measure> {

    public ArrayListOfResultsAdapter(Context context, int textViewResourceId,
                                     ArrayList<LeveOrthogonal.Measure> results) {
        super(context, textViewResourceId, results);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.leve_ortho_results_list_item, null);
        }

        LeveOrthogonal.Measure result = this.getItem(position);
        if (result != null) {
            TextView numberTextView = (TextView) view.findViewById(R.id.number_item);
            TextView abscissaTextView = (TextView) view.findViewById(R.id.abscissa_item);
            TextView ordinateTextView = (TextView) view.findViewById(R.id.ordinate_item);
            TextView vETextView = (TextView) view.findViewById(R.id.ve_item);
            TextView vNTextView = (TextView) view.findViewById(R.id.vn_item);

            if (numberTextView != null) {
                numberTextView.setText(result.getNumber());
            }

            if (abscissaTextView != null) {
                abscissaTextView.setText(DisplayUtils.formatCoordinate(result.getAbscissa()));
            }

            if (ordinateTextView != null) {
                ordinateTextView.setText(DisplayUtils.formatCoordinate(result.getOrdinate()));
            }

            if (vETextView != null) {
                vETextView.setText(DisplayUtils.formatGap(result.getvE()));
            }

            if (ordinateTextView != null) {
                vNTextView.setText(DisplayUtils.formatGap(result.getvN()));
            }
        }

        return view;
    }
}
