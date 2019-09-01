package com.linyulong.timepickerdemo;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * Created by liuwan on 2016/9/28.
 */
public class CustomDatePicker {

    /**
     * 定义结果回调接口
     */
    public interface ResultHandler {
        void handle(String time);
    }

    public enum SCROLL_TYPE {
        HOUR(1),
        MINUTE(2),
        SECOND(3);

        SCROLL_TYPE(int value) {
            this.value = value;
        }

        public int value;
    }

    private int scrollUnits = SCROLL_TYPE.HOUR.value + SCROLL_TYPE.MINUTE.value;
    private ResultHandler handler;
    private Context context;
    private boolean canAccess = false;

    private Dialog datePickerDialog;
    private DatePickerView pvYear, pvMonth, pvDay, pvHour, pvMinute, pvSecond;

    private static final int MAX_SECOND_OR_MINUTE = 59;
    private static final int MAX_HOUR = 23;
    private static final int MIN_SECOND_OR_MINUTE = 0;
    private static final int MIN_HOUR = 0;
    private static final int MAX_MONTH = 12;

    private ArrayList<String> yearList, monthList, dayList, hourList, minuteList, secondList;
    private int startYear, startMonth, startDay, startHour, startMinute, startSecond, endYear, endMonth, endDay,
            endHour, endMinute, endSecond;
    private int lastMonthDays; //上一个被选中的月份天数
    private String title;
    private String currentMon, currentDay, currentHour, currentMin, currentSecond; //当前选中的月、日、时、分
    private boolean spanYear, spanMon, spanDay, spanHour, spanMin, spanSecond;
    private Calendar selectedCalender, startCalendar, endCalendar;
    private TextView tvTitle, tvCancle, tvSelect, tvHour, tvMinute, tvSecond;

    public CustomDatePicker(Context context, String title, ResultHandler resultHandler, String startDate, String endDate) {
        if (isValidDate(startDate, "yyyy-MM-dd HH:mm:ss") && isValidDate(endDate, "yyyy-MM-dd HH:mm:ss")) {
            canAccess = true;
            this.context = context;
            this.handler = resultHandler;
            this.title = title;
            selectedCalender = Calendar.getInstance();
            startCalendar = Calendar.getInstance();
            endCalendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            try {
                startCalendar.setTime(sdf.parse(startDate));
                endCalendar.setTime(sdf.parse(endDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            initDialog();
            initView();
        }
    }

    private void initDialog() {
        if (datePickerDialog == null) {
            datePickerDialog = new Dialog(context, R.style.TimePickerDialog);
            datePickerDialog.setCancelable(true);
            datePickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            datePickerDialog.setContentView(R.layout.custom_date_picker);
            Window window = datePickerDialog.getWindow();
            window.setGravity(Gravity.BOTTOM);
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(dm);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = dm.widthPixels;
            window.setAttributes(lp);
        }
    }

    private void initView() {
        pvYear = (DatePickerView) datePickerDialog.findViewById(R.id.year_pv);
        pvMonth = (DatePickerView) datePickerDialog.findViewById(R.id.month_pv);
        pvDay = (DatePickerView) datePickerDialog.findViewById(R.id.day_pv);
        pvHour = (DatePickerView) datePickerDialog.findViewById(R.id.hour_pv);
        pvMinute = (DatePickerView) datePickerDialog.findViewById(R.id.minute_pv);
        pvSecond = (DatePickerView) datePickerDialog.findViewById(R.id.second_pv);
        tvTitle = (TextView) datePickerDialog.findViewById(R.id.tv_title);
        tvCancle = (TextView) datePickerDialog.findViewById(R.id.tv_cancle);
        tvSelect = (TextView) datePickerDialog.findViewById(R.id.tv_select);
        tvHour = (TextView) datePickerDialog.findViewById(R.id.hour_text);
        tvMinute = (TextView) datePickerDialog.findViewById(R.id.minute_text);
        tvSecond = (TextView) datePickerDialog.findViewById(R.id.second_text);

        tvTitle.setText(title);
        tvCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.dismiss();
            }
        });

        tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                handler.handle(sdf.format(selectedCalender.getTime()));
                datePickerDialog.dismiss();
            }
        });
    }

    private void initParameter() {
        startYear = startCalendar.get(Calendar.YEAR);
        startMonth = startCalendar.get(Calendar.MONTH) + 1;
        startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
        startHour = startCalendar.get(Calendar.HOUR_OF_DAY);
        startMinute = startCalendar.get(Calendar.MINUTE);
        startSecond = startCalendar.get(Calendar.SECOND);

        endYear = endCalendar.get(Calendar.YEAR);
        endMonth = endCalendar.get(Calendar.MONTH) + 1;
        endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
        endHour = endCalendar.get(Calendar.HOUR_OF_DAY);
        endMinute = endCalendar.get(Calendar.MINUTE);
        endSecond = endCalendar.get(Calendar.SECOND);

        spanYear = startYear != endYear;
        spanMon = (!spanYear) && (startMonth != endMonth);
        spanDay = (!spanMon) && (startDay != endDay);
        spanHour = (!spanDay) && (startHour != endHour);
        spanMin = (!spanHour) && (startMinute != endMinute);
        spanSecond = (!spanMin) && (startSecond != endSecond);
        selectedCalender.setTime(startCalendar.getTime());
    }

    private void initTimer() {
        initArrayList();
        Log.i(TAG, "initTimer: "+scrollUnits+"====="+(scrollUnits & SCROLL_TYPE.HOUR.value)+"==="+SCROLL_TYPE.MINUTE.value);
        if (spanYear) {
            for (int i = startYear; i <= endYear; i++) {
                yearList.add(String.valueOf(i));
            }
            for (int i = startMonth; i <= MAX_MONTH; i++) {
                monthList.add(formatTimeUnit(i));
            }
            for (int i = startDay; i <= startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                dayList.add(formatTimeUnit(i));
            }

            if ((scrollUnits & SCROLL_TYPE.HOUR.value) != SCROLL_TYPE.HOUR.value) {
                hourList.add(formatTimeUnit(startHour));
            } else {
                for (int i = startHour; i <= MAX_HOUR; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            }

            if ((scrollUnits & SCROLL_TYPE.MINUTE.value) != SCROLL_TYPE.MINUTE.value) {
                minuteList.add(formatTimeUnit(startMinute));
            } else {
                for (int i = startMinute; i <= MAX_SECOND_OR_MINUTE; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            }

            if ((scrollUnits & SCROLL_TYPE.SECOND.value) != SCROLL_TYPE.SECOND.value) {
                secondList.add(formatTimeUnit(startSecond));
            } else {
                for (int i = startSecond; i <= MAX_SECOND_OR_MINUTE; i++) {
                    secondList.add(formatTimeUnit(i));
                }
            }
        } else if (spanMon) {
            yearList.add(String.valueOf(startYear));
            for (int i = startMonth; i <= endMonth; i++) {
                monthList.add(formatTimeUnit(i));
            }
            for (int i = startDay; i <= startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                dayList.add(formatTimeUnit(i));
            }

            if ((scrollUnits & SCROLL_TYPE.HOUR.value) != SCROLL_TYPE.HOUR.value) {
                hourList.add(formatTimeUnit(startHour));
            } else {
                for (int i = startHour; i <= MAX_HOUR; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            }

            if ((scrollUnits & SCROLL_TYPE.MINUTE.value) != SCROLL_TYPE.MINUTE.value) {
                minuteList.add(formatTimeUnit(startMinute));
            } else {
                for (int i = startMinute; i <= MAX_SECOND_OR_MINUTE; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            }
        } else if (spanDay) {
            yearList.add(String.valueOf(startYear));
            monthList.add(formatTimeUnit(startMonth));
            for (int i = startDay; i <= endDay; i++) {
                dayList.add(formatTimeUnit(i));
            }

            if ((scrollUnits & SCROLL_TYPE.HOUR.value) != SCROLL_TYPE.HOUR.value) {
                hourList.add(formatTimeUnit(startHour));
            } else {
                for (int i = startHour; i <= MAX_HOUR; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            }

            if ((scrollUnits & SCROLL_TYPE.MINUTE.value) != SCROLL_TYPE.MINUTE.value) {
                minuteList.add(formatTimeUnit(startMinute));
            } else {
                for (int i = startMinute; i <= MAX_SECOND_OR_MINUTE; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            }
        } else if (spanHour) {
            yearList.add(String.valueOf(startYear));
            monthList.add(formatTimeUnit(startMonth));
            dayList.add(formatTimeUnit(startDay));

            if ((scrollUnits & SCROLL_TYPE.HOUR.value) != SCROLL_TYPE.HOUR.value) {
                hourList.add(formatTimeUnit(startHour));
            } else {
                for (int i = startHour; i <= endHour; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            }

            if ((scrollUnits & SCROLL_TYPE.MINUTE.value) != SCROLL_TYPE.MINUTE.value) {
                minuteList.add(formatTimeUnit(startMinute));
            } else {
                for (int i = startMinute; i <= MAX_SECOND_OR_MINUTE; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            }
        } else if (spanMin) {
            yearList.add(String.valueOf(startYear));
            monthList.add(formatTimeUnit(startMonth));
            dayList.add(formatTimeUnit(startDay));
            hourList.add(formatTimeUnit(startHour));

            if ((scrollUnits & SCROLL_TYPE.MINUTE.value) != SCROLL_TYPE.MINUTE.value) {
                minuteList.add(formatTimeUnit(startMinute));
            } else {
                for (int i = startMinute; i <= endMinute; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            }
        }
        loadComponent();
    }

    /**
     * 将“0-9”转换为“00-09”
     */
    private String formatTimeUnit(int unit) {
        return unit < 10 ? "0" + String.valueOf(unit) : String.valueOf(unit);
    }

    private void initArrayList() {
        if (yearList == null) yearList = new ArrayList<>();
        if (monthList == null) monthList = new ArrayList<>();
        if (dayList == null) dayList = new ArrayList<>();
        if (hourList == null) hourList = new ArrayList<>();
        if (minuteList == null) minuteList = new ArrayList<>();
        if (secondList == null) secondList = new ArrayList<>();
        yearList.clear();
        monthList.clear();
        dayList.clear();
        hourList.clear();
        minuteList.clear();
        secondList.clear();
    }

    private void loadComponent() {
        pvYear.setData(yearList);
        pvMonth.setData(monthList);
        pvDay.setData(dayList);
        pvHour.setData(hourList);
        pvMinute.setData(minuteList);
        pvSecond.setData(secondList);
        pvYear.setSelected(0);
        pvMonth.setSelected(0);
        pvDay.setSelected(0);
        pvHour.setSelected(0);
        pvMinute.setSelected(0);
        pvSecond.setSelected(0);
        executeScroll();
    }

    private void addListener() {
        pvYear.setOnSelectListener(new DatePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectedCalender.set(Calendar.YEAR, Integer.parseInt(text));
                monthChange();
            }
        });

        pvMonth.setOnSelectListener(new DatePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectedCalender.set(Calendar.DAY_OF_MONTH, 1);
                selectedCalender.set(Calendar.MONTH, Integer.parseInt(text) - 1);
                currentMon = text; //保存选择的月份
                dayChange();
            }
        });

        pvDay.setOnSelectListener(new DatePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectedCalender.set(Calendar.DAY_OF_MONTH, Integer.parseInt(text));
                currentDay = text;//保存选择的日期
                hourChange();
            }
        });

        pvHour.setOnSelectListener(new DatePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectedCalender.set(Calendar.HOUR_OF_DAY, Integer.parseInt(text));
                currentHour = text; //保存选择的小时
                minuteChange();
            }
        });

        pvMinute.setOnSelectListener(new DatePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectedCalender.set(Calendar.MINUTE, Integer.parseInt(text));
                currentMin = text; //保存选择的分钟
                secondChange();
            }
        });

        pvSecond.setOnSelectListener(new DatePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectedCalender.set(Calendar.SECOND, Integer.parseInt(text));
                currentSecond = text;
            }
        });
    }

    private void monthChange() {
        monthList.clear();
        int selectedYear = selectedCalender.get(Calendar.YEAR);
        if (selectedYear == startYear) {
            for (int i = startMonth; i <= MAX_MONTH; i++) {
                monthList.add(formatTimeUnit(i));
            }
        } else if (selectedYear == endYear) {
            for (int i = 1; i <= endMonth; i++) {
                monthList.add(formatTimeUnit(i));
            }
        } else {
            for (int i = 1; i <= MAX_MONTH; i++) {
                monthList.add(formatTimeUnit(i));
            }
        }
//        selectedCalender.set(Calendar.MONTH, Integer.parseInt(monthList.get(0)) - 1);
        pvMonth.setData(monthList);
        if (monthList.size() < MAX_MONTH && Integer.valueOf(currentMon) > monthList.size()) {
            pvMonth.setSelected(monthList.size() - 1);
            selectedCalender.set(Calendar.DAY_OF_MONTH, 1);
            selectedCalender.set(Calendar.MONTH, monthList.size() - 1);
        } else {
            pvMonth.setSelected(currentMon);
            selectedCalender.set(Calendar.DAY_OF_MONTH, 1);
            selectedCalender.set(Calendar.MONTH, Integer.valueOf(currentMon) - 1);
        }
        executeAnimator(pvMonth);

        pvMonth.postDelayed(new Runnable() {
            @Override
            public void run() {
                dayChange();
            }
        }, 100);
    }

    private void dayChange() {
        dayList.clear();
        int selectedYear = selectedCalender.get(Calendar.YEAR);
        int selectedMonth = selectedCalender.get(Calendar.MONTH) + 1;
        if (selectedYear == startYear && selectedMonth == startMonth) {
            for (int i = startDay; i <= selectedCalender.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                dayList.add(formatTimeUnit(i));
            }
        } else if (selectedYear == endYear && selectedMonth == endMonth) {
            for (int i = 1; i <= endDay; i++) {
                dayList.add(formatTimeUnit(i));
            }
        } else {
            for (int i = 1; i <= selectedCalender.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                dayList.add(formatTimeUnit(i));
            }
        }
        pvDay.setData(dayList);
//        selectedCalender.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayList.get(0)));
//        pvDay.setSelected(0);
        if (dayList.size() < lastMonthDays && Integer.valueOf(currentDay) > dayList.size()) {
            pvDay.setSelected(dayList.size() - 1);
            currentDay = formatTimeUnit(dayList.size());
        } else {
            pvDay.setSelected(currentDay);
        }
        selectedCalender.set(Calendar.DAY_OF_MONTH, Integer.parseInt(currentDay));
        //重新赋值
        lastMonthDays = dayList.size();
        executeAnimator(pvDay);

        pvDay.postDelayed(new Runnable() {
            @Override
            public void run() {
                hourChange();
            }
        }, 100);
    }

    private void hourChange() {
        if ((scrollUnits & SCROLL_TYPE.HOUR.value) == SCROLL_TYPE.HOUR.value) {
            hourList.clear();
            int selectedYear = selectedCalender.get(Calendar.YEAR);
            int selectedMonth = selectedCalender.get(Calendar.MONTH) + 1;
            int selectedDay = selectedCalender.get(Calendar.DAY_OF_MONTH);
            if (selectedYear == startYear && selectedMonth == startMonth && selectedDay == startDay) {
                for (int i = startHour; i <= MAX_HOUR; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            } else if (selectedYear == endYear && selectedMonth == endMonth && selectedDay == endDay) {
                for (int i = MIN_HOUR; i <= endHour; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            } else {
                for (int i = MIN_HOUR; i <= MAX_HOUR; i++) {
                    hourList.add(formatTimeUnit(i));
                }
            }
//            selectedCalender.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourList.get(0)));
//            pvHour.setSelected(0);
            pvHour.setData(hourList);
            if (hourList.size() < 24 && Integer.valueOf(currentHour) > hourList.size()) {
                pvHour.setSelected(hourList.size() - 1);
                selectedCalender.set(Calendar.HOUR_OF_DAY, hourList.size());
                currentHour = formatTimeUnit(hourList.size());
            } else {
                pvHour.setSelected(currentHour);
                selectedCalender.set(Calendar.HOUR_OF_DAY, Integer.valueOf(currentHour));
            }
            executeAnimator(pvHour);
        }

        pvHour.postDelayed(new Runnable() {
            @Override
            public void run() {
                minuteChange();
            }
        }, 100);
    }

    private void minuteChange() {
        if ((scrollUnits & SCROLL_TYPE.MINUTE.value) == SCROLL_TYPE.MINUTE.value) {
            minuteList.clear();
            int selectedYear = selectedCalender.get(Calendar.YEAR);
            int selectedMonth = selectedCalender.get(Calendar.MONTH) + 1;
            int selectedDay = selectedCalender.get(Calendar.DAY_OF_MONTH);
            int selectedHour = selectedCalender.get(Calendar.HOUR_OF_DAY);
            if (selectedYear == startYear && selectedMonth == startMonth && selectedDay == startDay && selectedHour == startHour) {
                for (int i = startMinute; i <= MAX_SECOND_OR_MINUTE; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            } else if (selectedYear == endYear && selectedMonth == endMonth && selectedDay == endDay && selectedHour == endHour) {
                for (int i = MIN_SECOND_OR_MINUTE; i <= endMinute; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            } else {
                for (int i = MIN_SECOND_OR_MINUTE; i <= MAX_SECOND_OR_MINUTE; i++) {
                    minuteList.add(formatTimeUnit(i));
                }
            }
            pvMinute.setData(minuteList);
//            selectedCalender.set(Calendar.MINUTE, Integer.parseInt(minuteList.get(0)));
//            pvMinute.setSelected(0);
            if (minuteList.size() < 60 && minuteList.size() < Integer.valueOf(currentMin)) {
                pvMinute.setSelected(minuteList.size() - 1);
                selectedCalender.set(Calendar.MINUTE, minuteList.size());
                //改变当前选择的分钟
                currentMin = formatTimeUnit(minuteList.size());
            } else {
                pvMinute.setSelected(currentMin);
                selectedCalender.set(Calendar.MINUTE, Integer.parseInt(currentMin));
            }
            executeAnimator(pvMinute);
        }
        pvMinute.postDelayed(new Runnable() {
            @Override
            public void run() {
                secondChange();
            }
        }, 100);
    }

    private void secondChange() {
        if ((scrollUnits & SCROLL_TYPE.SECOND.value) == SCROLL_TYPE.SECOND.value) {
            secondList.clear();
            int selectedYear = selectedCalender.get(Calendar.YEAR);
            int selectedMonth = selectedCalender.get(Calendar.MONTH) + 1;
            int selectedDay = selectedCalender.get(Calendar.DAY_OF_MONTH);
            int selectedHour = selectedCalender.get(Calendar.HOUR_OF_DAY);
            int selectedMinute = selectedCalender.get(Calendar.MINUTE);
            if (selectedYear == startYear && selectedMonth == startMonth && selectedDay == startDay && selectedHour == startHour &&
                    selectedMinute == startMinute) {
                for (int i = startSecond; i <= MAX_SECOND_OR_MINUTE; i++) {
                    secondList.add(formatTimeUnit(i));
                }
            } else if (selectedYear == endYear && selectedMonth == endMonth && selectedDay == endDay && selectedHour == endHour &&
                    selectedMinute == endMinute) {
                for (int i = MIN_SECOND_OR_MINUTE; i <= endMinute; i++) {
                    secondList.add(formatTimeUnit(i));
                }
            } else {
                for (int i = MIN_SECOND_OR_MINUTE; i <= MAX_SECOND_OR_MINUTE; i++) {
                    secondList.add(formatTimeUnit(i));
                }
            }
            pvSecond.setData(secondList);
//            selectedCalender.set(Calendar.MINUTE, Integer.parseInt(minuteList.get(0)));
//            pvMinute.setSelected(0);
            if (secondList.size() < 60 && secondList.size() < Integer.valueOf(currentSecond)) {
                pvSecond.setSelected(secondList.size() - 1);
                selectedCalender.set(Calendar.SECOND, secondList.size());
                //改变当前选择的分钟
                currentSecond= formatTimeUnit(secondList.size());
            } else {
                pvSecond.setSelected(currentSecond);
                selectedCalender.set(Calendar.SECOND, Integer.parseInt(currentSecond));
            }
            executeAnimator(pvSecond);
        }
        executeScroll();
    }

    private void executeAnimator(View view) {
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("alpha", 1f, 0f, 1f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f, 1f);
        PropertyValuesHolder pvhZ = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f, 1f);
        ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY, pvhZ).setDuration(200).start();
    }

    private void executeScroll() {
        pvYear.setCanScroll(yearList.size() > 1);
        pvMonth.setCanScroll(monthList.size() > 1);
        pvDay.setCanScroll(dayList.size() > 1);
        pvHour.setCanScroll(hourList.size() > 1 && (scrollUnits & SCROLL_TYPE.HOUR.value) == SCROLL_TYPE.HOUR.value);
        pvMinute.setCanScroll(minuteList.size() > 1 && (scrollUnits & SCROLL_TYPE.MINUTE.value) == SCROLL_TYPE.MINUTE.value);
        pvSecond.setCanScroll(secondList.size() > 1 && (scrollUnits & SCROLL_TYPE.SECOND.value) == SCROLL_TYPE.SECOND.value);
    }

    private int disScrollUnit(SCROLL_TYPE... scroll_types) {
        if (scroll_types == null || scroll_types.length == 0) {
            scrollUnits = SCROLL_TYPE.HOUR.value + SCROLL_TYPE.MINUTE.value;
            Log.i(TAG, "disScrolfefelUnit: "+scrollUnits);
        } else {
            for (SCROLL_TYPE scroll_type : scroll_types) {
                scrollUnits ^= scroll_type.value;
            }
        }
        return scrollUnits;
    }

    public void show(String time) {
        if (canAccess) {
            if (isValidDate(time, "yyyy-MM-dd")) {
                if (startCalendar.getTime().getTime() < endCalendar.getTime().getTime()) {
                    canAccess = true;
                    initParameter();
                    initTimer();
                    addListener();
                    setSelectedTime(time);
                    datePickerDialog.show();
                }
            } else {
                canAccess = false;
            }
        }
    }

    /**
     * 设置日期控件是否显示时和分
     */
    public void showSpecificTime(boolean show) {
        if (canAccess) {
            if (show) {
                disScrollUnit();
                pvHour.setVisibility(View.VISIBLE);
                tvHour.setVisibility(View.VISIBLE);
                pvMinute.setVisibility(View.VISIBLE);
                tvMinute.setVisibility(View.VISIBLE);
                pvSecond.setVisibility(View.VISIBLE);
                tvSecond.setVisibility(View.VISIBLE);
            } else {
                disScrollUnit(SCROLL_TYPE.HOUR, SCROLL_TYPE.MINUTE, SCROLL_TYPE.SECOND);
                pvHour.setVisibility(View.GONE);
                tvHour.setVisibility(View.GONE);
                pvMinute.setVisibility(View.GONE);
                tvMinute.setVisibility(View.GONE);
                pvSecond.setVisibility(View.GONE);
                tvSecond.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置日期控件是否可以循环滚动
     */
    public void setIsLoop(boolean isLoop) {
        if (canAccess) {
            this.pvYear.setIsLoop(isLoop);
            this.pvMonth.setIsLoop(isLoop);
            this.pvDay.setIsLoop(isLoop);
            this.pvHour.setIsLoop(isLoop);
            this.pvMinute.setIsLoop(isLoop);
            this.pvSecond.setIsLoop(isLoop);
        }
    }

    public void setYearIsLoop(boolean isLoop) {
        if (canAccess) {
            this.pvYear.setIsLoop(isLoop);
        }
    }

    public void setMonIsLoop(boolean isLoop) {
        if (canAccess) {
            this.pvMonth.setIsLoop(isLoop);
        }
    }

    public void setDayIsLoop(boolean isLoop) {
        if (canAccess) {
            this.pvDay.setIsLoop(isLoop);
        }
    }

    public void setHourIsLoop(boolean isLoop) {
        if (canAccess) {
            this.pvHour.setIsLoop(isLoop);
        }
    }

    public void setMinIsLoop(boolean isLoop) {
        if (canAccess) {
            this.pvMinute.setIsLoop(isLoop);
        }
    }

    /**
     * 设置日期控件默认选中的时间
     */
    public void setSelectedTime(String time) {
        if (canAccess) {
            String[] str = time.split(" ");
            String[] dateStr = str[0].split("-");

            pvYear.setSelected(dateStr[0]);
            selectedCalender.set(Calendar.YEAR, Integer.parseInt(dateStr[0]));

            monthList.clear();
            int selectedYear = selectedCalender.get(Calendar.YEAR);
            if (selectedYear == startYear) {
                for (int i = startMonth; i <= MAX_MONTH; i++) {
                    monthList.add(formatTimeUnit(i));
                }
            } else if (selectedYear == endYear) {
                for (int i = 1; i <= endMonth; i++) {
                    monthList.add(formatTimeUnit(i));
                }
            } else {
                for (int i = 1; i <= MAX_MONTH; i++) {
                    monthList.add(formatTimeUnit(i));
                }
            }
            pvMonth.setData(monthList);
            pvMonth.setSelected(dateStr[1]);
            currentMon = dateStr[1]; //保存选择的月份
            selectedCalender.set(Calendar.MONTH, Integer.parseInt(dateStr[1]) - 1);
            executeAnimator(pvMonth);

            dayList.clear();
            int selectedMonth = selectedCalender.get(Calendar.MONTH) + 1;
            if (selectedYear == startYear && selectedMonth == startMonth) {
                for (int i = startDay; i <= selectedCalender.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                    dayList.add(formatTimeUnit(i));
                }
            } else if (selectedYear == endYear && selectedMonth == endMonth) {
                for (int i = 1; i <= endDay; i++) {
                    dayList.add(formatTimeUnit(i));
                }
            } else {
                for (int i = 1; i <= selectedCalender.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
                    dayList.add(formatTimeUnit(i));
                }
            }
            lastMonthDays = dayList.size();
            pvDay.setData(dayList);
            pvDay.setSelected(dateStr[2]);
            currentDay = dateStr[2]; //保存选择的日
            selectedCalender.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateStr[2]));
            executeAnimator(pvDay);

            if (str.length == 2) {
                String[] timeStr = str[1].split(":");

                if ((scrollUnits & SCROLL_TYPE.HOUR.value) == SCROLL_TYPE.HOUR.value) {
                    hourList.clear();
                    int selectedDay = selectedCalender.get(Calendar.DAY_OF_MONTH);
                    if (selectedYear == startYear && selectedMonth == startMonth && selectedDay == startDay) {
                        for (int i = startHour; i <= MAX_HOUR; i++) {
                            hourList.add(formatTimeUnit(i));
                        }
                    } else if (selectedYear == endYear && selectedMonth == endMonth && selectedDay == endDay) {
                        for (int i = MIN_HOUR; i <= endHour; i++) {
                            hourList.add(formatTimeUnit(i));
                        }
                    } else {
                        for (int i = MIN_HOUR; i <= MAX_HOUR; i++) {
                            hourList.add(formatTimeUnit(i));
                        }
                    }
                    pvHour.setData(hourList);
                    pvHour.setSelected(timeStr[0]);
                    currentHour = timeStr[0]; //保存选择的小时
                    selectedCalender.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr[0]));
                    executeAnimator(pvHour);
                }

                if ((scrollUnits & SCROLL_TYPE.MINUTE.value) == SCROLL_TYPE.MINUTE.value) {
                    minuteList.clear();
                    int selectedDay = selectedCalender.get(Calendar.DAY_OF_MONTH);
                    int selectedHour = selectedCalender.get(Calendar.HOUR_OF_DAY);
                    if (selectedYear == startYear && selectedMonth == startMonth && selectedDay == startDay && selectedHour == startHour) {
                        for (int i = startMinute; i <= MAX_SECOND_OR_MINUTE; i++) {
                            minuteList.add(formatTimeUnit(i));
                        }
                    } else if (selectedYear == endYear && selectedMonth == endMonth && selectedDay == endDay && selectedHour == endHour) {
                        for (int i = MIN_SECOND_OR_MINUTE; i <= endMinute; i++) {
                            minuteList.add(formatTimeUnit(i));
                        }
                    } else {
                        for (int i = MIN_SECOND_OR_MINUTE; i <= MAX_SECOND_OR_MINUTE; i++) {
                            minuteList.add(formatTimeUnit(i));
                        }
                    }
                    pvMinute.setData(minuteList);
                    pvMinute.setSelected(timeStr[1]);
                    currentMin = timeStr[1]; //保存选择的分钟
                    selectedCalender.set(Calendar.MINUTE, Integer.parseInt(timeStr[1]));
                    executeAnimator(pvMinute);
                }

                if ((scrollUnits & SCROLL_TYPE.SECOND.value) == SCROLL_TYPE.SECOND.value) {
                    secondList.clear();
                    int selectedDay = selectedCalender.get(Calendar.DAY_OF_MONTH);
                    int selectedHour = selectedCalender.get(Calendar.HOUR_OF_DAY);
                    int selectedMinute = selectedCalender.get(Calendar.MINUTE);
                    if (selectedYear == startYear && selectedMonth == startMonth && selectedDay == startDay && selectedHour == startHour &&
                            selectedMinute == startMinute) {
                        for (int i = startSecond; i <= MAX_SECOND_OR_MINUTE; i++) {
                            secondList.add(formatTimeUnit(i));
                        }
                    } else if (selectedYear == endYear && selectedMonth == endMonth && selectedDay == endDay && selectedHour == endHour &&
                            selectedMinute == endMinute) {
                        for (int i = MIN_SECOND_OR_MINUTE; i <= endSecond; i++) {
                            secondList.add(formatTimeUnit(i));
                        }
                    } else {
                        for (int i = MIN_SECOND_OR_MINUTE; i <= MAX_SECOND_OR_MINUTE; i++) {
                            secondList.add(formatTimeUnit(i));
                        }
                    }
                    pvSecond.setData(secondList);
                    pvSecond.setSelected(timeStr[2]);
                    currentSecond = timeStr[2]; //保存选择的分钟
                    selectedCalender.set(Calendar.SECOND, Integer.parseInt(timeStr[2]));
                    executeAnimator(pvSecond);
                }
            }
            executeScroll();
        }
    }

    /**
     * 验证字符串是否是一个合法的日期格式
     */
    private boolean isValidDate(String date, String template) {
        boolean convertSuccess = true;
        // 指定日期格式
        SimpleDateFormat format = new SimpleDateFormat(template, Locale.CHINA);
        try {
            // 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2015/02/29会被接受，并转换成2015/03/01
            format.setLenient(false);
            format.parse(date);
        } catch (Exception e) {
            // 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
            convertSuccess = false;
        }
        return convertSuccess;
    }
}
