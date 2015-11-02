package net.nud.basemaps;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

public class LoginDialogFragment extends android.support.v4.app.DialogFragment {

    /*
    Whichever activity that uses this fragment must implement this interface
*/
    public interface LoginDialogListener{
        void onLoginPositiveClick(DialogFragment dialog);
        void onLoginNegativeClick(DialogFragment dialog);
    }

    LoginDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (LoginDialogListener) activity;
        } catch (ClassCastException ex) {
            throw new ClassCastException(activity.toString() + "Must implement LoginDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_signin, null))
                // Add action buttons
                .setPositiveButton(R.string.signin, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        mListener.onLoginPositiveClick(LoginDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onLoginNegativeClick(LoginDialogFragment.this);
                    }
                });
        return builder.create();

    }
}