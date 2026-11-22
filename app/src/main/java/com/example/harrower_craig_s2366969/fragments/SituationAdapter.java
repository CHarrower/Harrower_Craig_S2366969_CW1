package com.example.harrower_craig_s2366969.fragments;

//
// Name                 Craig Harrower
// Student ID           S2366969
// Programme of Study   BSc Software Development
//

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.harrower_craig_s2366969.R;
import com.example.harrower_craig_s2366969.model.Situation;

import java.util.ArrayList;

// custom adapter for the incidents / roadworks lists (lecture 3a, ListView_custom - CustomAdapter)
// each row is a delay colour bar + an icon + the type + the comment
// icons: Font Awesome Free (fontawesome.com), CC BY 4.0 (see Coursework CW1/Icons/sources.txt)
public class SituationAdapter extends BaseAdapter
{
    private final ArrayList<Situation> situations;   // the data for the rows
    private final LayoutInflater inflter;
    private final Context context;                   // needed to read the colour resources

    public SituationAdapter(Context applicationContext, ArrayList<Situation> situations)
    {
        this.situations = situations;
        this.context = applicationContext;
        inflter = LayoutInflater.from(applicationContext);
    }

    @Override
    public int getCount() { return situations.size(); }

    @Override
    public Object getItem(int i) { return situations.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    // getView() creates and fills in the layout for each row (with the widgets holder from lecture 3a)
    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        WidgetsHolder holder;
        if (view == null) { // 1st time - create the row and bind to its widgets
            view = inflter.inflate(R.layout.list_item_situation, null);
            holder = new WidgetsHolder();
            holder.type = view.findViewById(R.id.typeText);
            holder.comment = view.findViewById(R.id.commentText);
            holder.icon = view.findViewById(R.id.icon);
            holder.delayBar = view.findViewById(R.id.delayBar);
            holder.dates = view.findViewById(R.id.datesText);
            holder.badge = view.findViewById(R.id.delayBadge);
            view.setTag(holder); // keep the bound widgets with the row
        }
        else {
            holder = (WidgetsHolder) view.getTag();
        }
        Situation situation = situations.get(i);
        String delays = readableDelays(situation.getDelaysType());
        String title = readableType(situation);
        if (!situation.getRoad().isEmpty()) {
            title = situation.getRoad() + " · " + title; // road first, easier to scan
        }
        holder.type.setText(title);
        // delay level as a small coloured badge - in words too, not just colour
        if (delays.isEmpty()) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(delays.substring(0, 1).toUpperCase() + delays.substring(1)); // "long delays" -> "Long delays"
            holder.badge.setBackgroundResource(badgeBackground(situation.getDelaysType()));
            holder.badge.setTextColor(context.getResources().getColor(badgeTextColour(situation.getDelaysType()), null));
        }
        holder.comment.setText(situation.getComment());
        holder.dates.setText(dateRange(situation));
        holder.icon.setImageResource(iconFor(situation));
        holder.delayBar.setBackgroundColor(context.getResources().getColor(delayColour(situation.getDelaysType()), null)); // week 2 colour resources
        return view; // ready-made row to show
    }

    // e.g. "roadClosed" -> "Road closed" - uses the sub-type if there is one, otherwise the record type
    public static String readableType(Situation situation)
    {
        String type = situation.getSubType();
        if (type == null) type = situation.getRecordType();
        if (type == null || type.isEmpty()) return "Unknown";
        String spaced = type.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    // pick an icon for the type of situation
    public static int iconFor(Situation situation)
    {
        String record = situation.getRecordType() == null ? "" : situation.getRecordType();
        String type = situation.getSubType() == null ? "" : situation.getSubType();
        String comment = situation.getComment() == null ? "" : situation.getComment().toLowerCase();

        if (record.equals("Accident")) return R.drawable.ic_accident;
        if (record.contains("Obstruction") || type.toLowerCase().contains("obstruction")) return R.drawable.ic_obstruction;
        if (record.equals("AbnormalTraffic")) return R.drawable.ic_queuing_traffic;
        if (type.equals("roadClosed")) return R.drawable.ic_road_closed;
        if (type.toLowerCase().contains("lane")) return R.drawable.ic_lane_closure;
        if (record.equals("MaintenanceWorks")) {
            // roadworks - use the comment to pick a more specific icon
            if (comment.contains("lane closure")) return R.drawable.ic_lane_closure;
            if (comment.contains("closure")) return R.drawable.ic_road_closed;
            if (comment.contains("lights") || comment.contains("ttl")) return R.drawable.ic_traffic_lights;
            return R.drawable.ic_roadworks;
        }
        return R.drawable.ic_other;
    }

    // colour for the delay bar - short delays amber, long orange, very long red, none = no bar
    public static int delayColour(String delaysType)
    {
        if (delaysType == null) return R.color.delay_none;
        if (delaysType.equals("veryLongDelays")) return R.color.delay_very_long;
        if (delaysType.equals("longDelays")) return R.color.delay_long;
        if (delaysType.equals("delays")) return R.color.delay_short;
        return R.color.delay_none;
    }

    // delay in words for the row, e.g. "long delays"
    public static String readableDelays(String delaysType)
    {
        if (delaysType == null) return "";
        if (delaysType.equals("veryLongDelays")) return "very long delays";
        if (delaysType.equals("longDelays")) return "long delays";
        if (delaysType.equals("delays")) return "short delays";
        return "";
    }

    // box + colour for the delay badge, same levels as the delay bar
    private static int badgeBackground(String delaysType)
    {
        if ("veryLongDelays".equals(delaysType)) return R.drawable.badge_very_long;
        if ("longDelays".equals(delaysType)) return R.drawable.badge_long;
        return R.drawable.badge_short;
    }

    private static int badgeTextColour(String delaysType)
    {
        if ("veryLongDelays".equals(delaysType)) return R.color.badge_very_long_text;
        if ("longDelays".equals(delaysType)) return R.color.badge_long_text;
        return R.color.badge_short_text;
    }

    // "22/07/2023 - 31/12/2027" for the list row (just the dates, the times are on the detail screen)
    private static String dateRange(Situation situation)
    {
        String start = justDate(situation.getStartTime());
        if (situation.getEndTime() == null) {
            return "From " + start; // incidents usually have no end date
        }
        return start + " - " + justDate(situation.getEndTime());
    }

    // "22/07/2023 at 00:00" -> "22/07/2023"
    private static String justDate(String dateTime)
    {
        String readable = DetailFragment.readableDate(dateTime);
        int at = readable.indexOf(" at ");
        if (at < 0) return readable; // "Not given"
        return readable.substring(0, at);
    }

    static class WidgetsHolder { // keeps the widgets bound to each row
        TextView type;
        TextView comment;
        TextView dates;
        TextView badge;
        ImageView icon;
        View delayBar;
    }
}
