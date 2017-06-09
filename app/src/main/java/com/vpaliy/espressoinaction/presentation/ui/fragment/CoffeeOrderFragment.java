package com.vpaliy.espressoinaction.presentation.ui.fragment;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.jesusm.holocircleseekbar.lib.HoloCircleSeekBar;
import com.vpaliy.espressoinaction.CoffeeApp;
import com.vpaliy.espressoinaction.R;
import com.vpaliy.espressoinaction.common.Constants;
import com.vpaliy.espressoinaction.databinding.FragmentOrderFormBinding;
import com.vpaliy.espressoinaction.databinding.LayoutOrderConfirmationBinding;
import com.vpaliy.espressoinaction.di.component.DaggerViewComponent;
import com.vpaliy.espressoinaction.di.module.PresenterModule;
import com.vpaliy.espressoinaction.domain.model.Coffee;
import com.vpaliy.espressoinaction.domain.model.CoffeeType;
import com.vpaliy.espressoinaction.domain.model.MilkType;
import com.vpaliy.espressoinaction.domain.model.SizeType;
import com.vpaliy.espressoinaction.domain.model.Sweetness;
import com.vpaliy.espressoinaction.presentation.mvp.contract.CoffeeOrderContract;
import com.vpaliy.espressoinaction.presentation.mvp.contract.CoffeeOrderContract.Presenter;
import com.vpaliy.espressoinaction.presentation.ui.utils.CalendarUtils;
import com.vpaliy.espressoinaction.presentation.ui.utils.ConvertUtils;
import com.vpaliy.espressoinaction.presentation.ui.utils.TextUtils;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import javax.inject.Inject;

public class CoffeeOrderFragment extends BottomSheetDialogFragment
        implements CoffeeOrderContract.View{

    private Presenter presenter;
    private FragmentOrderFormBinding binding;
    private Map<View,Pair<View,View>> clonedViewsMap=new LinkedHashMap<>();
    private Result result=new Result();
    private int coffeeId;

    public interface OnPropertyClicked {
        void onClick(View view);
    }

    public interface TextHandler {
        CharSequence setText(String leftPart, String rightPart);
    }

    public static class Result{
        public String size;
        public Drawable sizeImage;
        public String milkType;
        public Drawable milkImage;
        public String day;
        public String time;
        public String title;
        public String price;
        public String sugarType;
        public String coffeeImage;

        @BindingAdapter({"bind:imageUrl"})
        public static void loadInto(ImageView image, String imageUrl){
            int resourceId = Integer.parseInt(imageUrl.substring(imageUrl.lastIndexOf("/") + 1));
            Glide.with(image.getContext())
                    .load(resourceId)
                    .centerCrop()
                    .into(image);
        }
    }

    @Override
    public void showCustomizedCoffee(Coffee coffee) {
        result.title=coffee.getCoffeeType().name;
        result.milkType= ConvertUtils.getMilkTypeLabel(getContext(),coffee.getMilkType());
        result.milkImage=ConvertUtils.getMilkImage(getContext(),coffee.getMilkType());
        result.size=ConvertUtils.getSizeTypeLabel(getContext(),coffee.getSizeType());
        result.sizeImage=ConvertUtils.getSizeTypeImage(getContext(),coffee.getSizeType());
        result.price=String.format(Locale.US,"$%.0f",coffee.getPrice());
        result.sugarType=ConvertUtils.getSweetnessText(getContext(),coffee.getSweetness());
        result.coffeeImage=coffee.getImageUrl();
        changeToConfirmScene();
    }

    public static CoffeeOrderFragment newInstance(Bundle args){
        CoffeeOrderFragment fragment=new CoffeeOrderFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void initializeDependencies() {
        DaggerViewComponent.builder()
                .presenterModule(new PresenterModule())
                .applicationComponent(CoffeeApp.app().component())
                .build().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        initializeDependencies();
        if(savedInstanceState==null) {
            savedInstanceState=getArguments();
        }
        this.coffeeId = savedInstanceState.getInt(Constants.EXTRA_COFFEE_ID);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding=DataBindingUtil.inflate(inflater,R.layout.fragment_order_form,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(view!=null){
            presenter.start(coffeeId);
            stepOne();
        }
    }

    private void stepOne(){
        binding.goButton.setOnClickListener(v-> stepTwo());
        binding.layoutOne.setTextHandler((leftPart, rightPart) ->
            TextUtils.mergeColoredText(leftPart,rightPart,
                    ContextCompat.getColor(getContext(),R.color.colorPrimary),
                    ContextCompat.getColor(getContext(),R.color.colorPrice)));
        binding.layoutOne.setMilkHandler(text-> {
            removeIfExists(binding.propertyLabelTwo);
            text.setSelected(true);
            switch (text.getId()){
                case R.id.almond_milk:
                    presenter.onMilkTypeSelected(MilkType.ALMOND);
                    break;
                case R.id.cow_milk:
                    presenter.onMilkTypeSelected(MilkType.WHOLE_MILK);
                    break;
                case R.id.coconut_milk:
                    presenter.onMilkTypeSelected(MilkType.COCONUT);
                    break;
                case R.id.soy_milk:
                    presenter.onMilkTypeSelected(MilkType.SOY);
                    break;
            }
            animateIcon(createIconView(text,binding.propertyLabelTwo),binding.propertyLabelTwo);
        });
        binding.layoutOne.setSizeHandler(text-> {
            removeIfExists(binding.propertyLabelOne);
            switch (text.getId()){
                case R.id.small_cup_size:
                    presenter.onSizeTypeSelected(SizeType.SMALL);
                    break;
                case R.id.medium_size_cup:
                    presenter.onSizeTypeSelected(SizeType.MEDIUM);
                    break;
                case R.id.large_size_cup:
                    presenter.onSizeTypeSelected(SizeType.LARGE);
                    break;
                case R.id.tall_size_cup:
                    presenter.onSizeTypeSelected(SizeType.TALL);
                    break;
            }
            text.setSelected(true);
            animateIcon(createIconView(text,binding.propertyLabelOne), binding.propertyLabelOne);
        });
    }

    private void stepTwo(){
        binding.switcher.showNext();
        cleanUpOnFlip();
        binding.propertyLabelOne.setVisibility(View.GONE);
        binding.propertyLabelTwo.setVisibility(View.GONE);
        binding.goButton.setOnClickListener(v-> stepThree());
        binding.layoutTwo.picker.setOnSeekBarChangeListener(new HoloCircleSeekBar.OnCircleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(HoloCircleSeekBar holoCircleSeekBar, int position, boolean wtf) {}
            @Override
            public void onStartTrackingTouch(HoloCircleSeekBar holoCircleSeekBar) {
                ViewCompat.animate(binding.layoutTwo.sugarLevel)
                        .alpha(0f)
                        .setDuration(200)
                        .start();
            }

            @Override
            public void onStopTrackingTouch(HoloCircleSeekBar holoCircleSeekBar) {
                int percent=holoCircleSeekBar.getValue();
                for(Sweetness value:Sweetness.values()){
                    if(value.percent>=percent){
                        presenter.onSweetnessTypeSelected(value);
                        binding.layoutTwo.sugarLevel.setText(ConvertUtils.getSweetnessText(getContext(),value));
                        break;
                    }
                }
                ViewCompat.animate(binding.layoutTwo.sugarLevel)
                        .alpha(1f)
                        .setDuration(200)
                        .start();
            }
        });
    }

    private void stepThree(){
        cleanUpOnFlip();
        binding.layoutTwo.picker.setVisibility(View.INVISIBLE);
        binding.propertyLabelOne.setVisibility(View.VISIBLE);
        binding.propertyLabelTwo.setVisibility(View.VISIBLE);
        prepareDay();
        prepareTime();
        binding.switcher.showNext();
        binding.goButton.setOnClickListener(v->presenter.onFinish());
        binding.layoutThree.setDayHandler(day->{
            removeIfExists(binding.propertyLabelOne);
            day.setSelected(true);
            TextView timeText=createTimeView(day,binding.propertyLabelOne);
            result.day=TextUtils.toSingleLine(timeText.getText());
            animateText(timeText,binding.propertyLabelOne);
        });
        binding.layoutThree.setTimeHandler(time->{
            removeIfExists(binding.propertyLabelTwo);
            time.setSelected(true);
            TextView timeText=createTimeView(time,binding.propertyLabelTwo);
            result.time=TextUtils.toSingleLine(timeText.getText());
            animateText(timeText, binding.propertyLabelTwo);
        });
    }

    private void removeIfExists(View key){
        if(clonedViewsMap.containsKey(key)) {
            Pair<View, View> pair = clonedViewsMap.get(key);
            pair.first.setSelected(false);
            binding.mainContainer.removeView(pair.second);
            clonedViewsMap.remove(key);
        }
    }

    @Override
    public void showCoffee(@NonNull Coffee coffee) {
        showCoffeeImage(coffee);
        showCoffeePrice(coffee.getPrice());
        showCoffeeType(coffee.getCoffeeType());
    }

    private void showCoffeeType(CoffeeType coffeeType){
        binding.coffeeName.setText(coffeeType.name);
    }


    private void showCoffeeImage(Coffee coffee) {
        String drawableUri = coffee.getImageUrl();
        int resourceId = Integer.parseInt(drawableUri.substring(drawableUri.lastIndexOf("/") + 1));
        Glide.with(getContext())
                .load(resourceId)
                .centerCrop()
                .into(binding.coffeeImage);
    }

    private void showCoffeePrice(double price){
        String textPrice=String.format(Locale.US,"$ "+"%.0f",price);
        binding.coffeePrice.setText(textPrice);
    }

    @Override
    public void showMessage(String message) {
    }

    private void changeToConfirmScene() {
        LayoutOrderConfirmationBinding confBinding=prepareConfirmation();
        final Scene scene = new Scene(binding.content,
                ((ViewGroup) confBinding.getRoot()));
        scene.setEnterAction(()-> ViewCompat.animate(confBinding.txtSubtitle)
                .scaleX(1.5f).scaleY(1.5f)
                .setInterpolator(new OvershootInterpolator())
                .setStartDelay(200)
                .start());
        final Transition transition = TransitionInflater.from(getContext())
                .inflateTransition(R.transition.transition_confirmation_view);
        TransitionManager.go(scene, transition);
    }

    private LayoutOrderConfirmationBinding prepareConfirmation(){
        LayoutOrderConfirmationBinding confirmationBinding=LayoutOrderConfirmationBinding
                .inflate(LayoutInflater.from(getContext()),binding.mainContainer,false);
        confirmationBinding.setResult(result);
        return confirmationBinding;
    }

    private void cleanUpOnFlip(){
        if(clonedViewsMap.size()!=0){
            clonedViewsMap.forEach((key,pair)->
                binding.mainContainer.removeView(pair.second));
            clonedViewsMap.clear();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.stop();
    }

    private void prepareDay(){
        Calendar calendar=Calendar.getInstance();
        for(int index=0;index<4;index++) {
            TextView day;
            switch (index){
                case 0:
                    day=binding.layoutThree.todayDay;
                    break;
                case 1:
                    day=binding.layoutThree.tomorrowDay;
                    break;
                case 2:
                    day=binding.layoutThree.thirdDay;
                    break;
                default:
                    day=binding.layoutThree.fourthDay;
                    break;
            }
            String textDay=CalendarUtils.getDay(getContext(), calendar);
            textDay+='\n';
            textDay+=CalendarUtils.dayOfMonth(calendar);
            day.setText(CalendarUtils.buildSpannableText(textDay, 3));
            calendar.add(Calendar.DAY_OF_MONTH,1);
        }
    }

    private void prepareTime(){
        binding.propertyLabelOne.setText(getString(R.string.day_label));
        binding.propertyLabelTwo.setText(getString(R.string.time_label));
        for(int index=0;index<4;index++){
            TextView time;
            String text;
            switch (index){
                case 0:
                    time=binding.layoutThree.timeFrameOne;
                    text=time.getText().toString();
                    time.setText(CalendarUtils.buildSpannableText(text,4));
                    break;
                case 1:
                    time=binding.layoutThree.timeFrameTwo;
                    text=time.getText().toString();
                    time.setText(CalendarUtils.buildSpannableText(text,4));
                    break;
                case 2:
                    time=binding.layoutThree.timeFrameThree;
                    text=time.getText().toString();
                    time.setText(CalendarUtils.buildSpannableText(text,3));
                    break;
                default:
                    time=binding.layoutThree.timeFrameFour;
                    text=time.getText().toString();
                    time.setText(CalendarUtils.buildSpannableText(text,3));
                    break;
            }
        }
    }

    private ViewGroup getRoot(){
        return ViewGroup.class.cast(getView());
    }

    private void animateIcon(View clonedView, View targetView){
        clonedView.post(()->{
            TransitionManager.beginDelayedTransition(getRoot());
            clonedView.setLayoutParams(SelectedParamsFactory.endIconParams(clonedView,targetView));
        });
    }

    private void animateText(TextView clonedView, TextView targetView){
        clonedView.post(()->{
            TransitionManager.beginDelayedTransition(getRoot());
            clonedView.setLayoutParams(SelectedParamsFactory.endTextParams(clonedView,targetView));
            clonedView.setText(TextUtils.toSingleLine(clonedView.getText()));
        });
    }

    private TextView createIconView(View real, TextView textLabel){
        final TextView fakeSelectedTextView = new TextView(getContext());
        TextView realTextView=TextView.class.cast(real);
        Drawable[] drawables=realTextView.getCompoundDrawables();
        fakeSelectedTextView.setCompoundDrawables(drawables[0],drawables[1],drawables[2],drawables[3]);
        fakeSelectedTextView.setPadding(0,0,0,0);
        fakeSelectedTextView.setLayoutParams(SelectedParamsFactory.startTextParams(real,binding));
        Pair<View,View> pair=new Pair<>(real,fakeSelectedTextView);
        clonedViewsMap.put(textLabel,pair);
        binding.mainContainer.addView(fakeSelectedTextView);
        return fakeSelectedTextView;
    }

    private TextView createTimeView(View real, TextView textLabel){
        final TextView fakeSelectedTextView = new TextView(getContext(),null,R.attr.textStyle);
        TextView realTextView=TextView.class.cast(real);
        fakeSelectedTextView.setText(realTextView.getText());
        fakeSelectedTextView.setLayoutParams(SelectedParamsFactory.startTextParams(real,binding));
        Pair<View,View> pair=new Pair<>(real,fakeSelectedTextView);
        clonedViewsMap.put(textLabel,pair);
        binding.mainContainer.addView(fakeSelectedTextView);
        return fakeSelectedTextView;
    }

    @Override
    public void appendSizeCharge(double original, double additional) {
        String leftText=String.format(Locale.US,"$"+"%.0f",original);
        String rightText=String.format(Locale.US," + $"+"%.0f",additional);
        binding.coffeePrice.setText(TextUtils.mergeColoredText(leftText,rightText,
                ContextCompat.getColor(getContext(),R.color.colorPrimary),
                ContextCompat.getColor(getContext(),R.color.colorPrice)));
    }

    @Override
    public void showUpdatedPrice(double price) {
        showCoffeePrice(price);
    }

    @Inject
    @Override
    public void attachPresenter(@NonNull Presenter presenter) {
        this.presenter=presenter;
        this.presenter.attachView(this);
    }

    private static class SelectedParamsFactory {
        private static ConstraintLayout.LayoutParams startTextParams(View selectedView, FragmentOrderFormBinding binding) {
            final ConstraintLayout.LayoutParams layoutParams =
                    new ConstraintLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.topToTop = binding.mainContainer.getId();
            layoutParams.leftToLeft = binding.mainContainer.getId();
            layoutParams.setMargins((int) selectedView.getX(), (int) selectedView.getY(), 0, 0);
            setStartState(selectedView,layoutParams);
            return layoutParams;
        }

        private static void setStartState(View selectedView, ConstraintLayout.LayoutParams layoutParams) {
            layoutParams.topToTop = ((ViewGroup) selectedView.getParent().getParent()).getId();
            layoutParams.leftToLeft = ((ViewGroup) selectedView.getParent().getParent()).getId();
            layoutParams.setMargins((int) selectedView.getX(), (int) selectedView.getY(), 0, 0);
        }

        private static ConstraintLayout.LayoutParams endIconParams(View v, View targetView) {
            final int marginLeft = v.getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.spacing_medium);
            final int marginTop=v.getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.spacing_big);
            return endParams(v,targetView,marginTop,marginLeft);
        }

        private static ConstraintLayout.LayoutParams endParams(View view, View targetView,
                                                               int marginTop, int marginLeft){
            final ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) view.getLayoutParams();
            layoutParams.setMargins(marginLeft, marginTop, 0, 0);
            layoutParams.topToTop = targetView.getId();
            layoutParams.startToEnd = targetView.getId();
            layoutParams.bottomToBottom = targetView.getId();
            return layoutParams;
        }

        private static ConstraintLayout.LayoutParams endTextParams(View v, View targetView) {
            final int marginLeft = v.getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.spacing_medium);
            return endParams(v,targetView,0,marginLeft);
        }
    }
}
