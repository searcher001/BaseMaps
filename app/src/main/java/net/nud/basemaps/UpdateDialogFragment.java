package net.nud.basemaps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

public class UpdateDialogFragment extends android.support.v4.app.DialogFragment {

    /*
    Whichever activity that uses this fragment must implement this interface
    */
    public interface UpdateDialogListener{
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private UpdateDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (UpdateDialogListener) activity;
        } catch (ClassCastException ex) {
            throw new ClassCastException(activity.toString() + "MUST IMPLEMENT UPDATEDIALOGLISTENER");
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the Layout Inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the Dialog
        // pass null as the parent view because its going to the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_update, null))
                // Add action buttons
                .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        mListener.onDialogPositiveClick(UpdateDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(UpdateDialogFragment.this);
                    }
                });
        return builder.create();
    }

}
