package ch.hgdev.toposuite.calculation.activities.cheminortho;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import ch.hgdev.toposuite.App;
import ch.hgdev.toposuite.R;
import ch.hgdev.toposuite.SharedResources;
import ch.hgdev.toposuite.calculation.CheminementOrthogonal;
import ch.hgdev.toposuite.calculation.CheminementOrthogonal.Result;
import ch.hgdev.toposuite.points.Point;
import ch.hgdev.toposuite.utils.DisplayUtils;

/**
 * TODO refactoring with the-almost-same-class
 * {@link ch.hgdev.toposuite.calculation.activities.leveortho.ArrayListOfResultsAdapter}
 * 
 * @author HGdev
 * 
 */
public class ArrayListOfResultsAdapter extends ArrayAdapter<CheminementOrthogonal.Result> {
    private final ArrayList<CheminementOrthogonal.Result> results;

    public ArrayListOfResultsAdapter(Context context, int textViewResourceId,
            ArrayList<CheminementOrthogonal.Result> results) {
        super(context, textViewResourceId, results);
        this.results = results;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.leve_ortho_results_list_item, null);
        }

        CheminementOrthogonal.Result result = this.results.get(position);
        if (result != null) {
            TextView numberTextView = (TextView) view.findViewById(R.id.number_item);
            TextView abscissaTextView = (TextView) view.findViewById(R.id.abscissa_item);
            TextView ordinateTextView = (TextView) view.findViewById(R.id.ordinate_item);
            TextView vETextView = (TextView) view.findViewById(R.id.ve_item);
            TextView vNTextView = (TextView) view.findViewById(R.id.vn_item);

            if (numberTextView != null) {
                String numberText = DisplayUtils.toString(result.getNumber()) + " ";
                Result r = this.getItem(position);
                Point point = new Point(
                        r.getNumber(),
                        r.getEast(),
                        r.getNorth(),
                        0.0,
                        false);
                Point p = SharedResources.getSetOfPoints().find(r.getNumber());
                numberText += point.equals(p) ?
                        App.getContext().getString(R.string.heavy_checkmark) :
                        App.getContext().getString(R.string.heavy_ballot);
                numberTextView.setText(numberText);
            }

            if (abscissaTextView != null) {
                abscissaTextView.setText(DisplayUtils.toString(result.getEast()));
            }

            if (ordinateTextView != null) {
                ordinateTextView.setText(DisplayUtils.toString(result.getNorth()));
            }

            if (vETextView != null) {
                vETextView.setText(DisplayUtils.toString(result.getvE()));
            }

            if (ordinateTextView != null) {
                vNTextView.setText(DisplayUtils.toString(result.getvN()));
            }
        }

        return view;
    }
}